package com.minpaku.client;

import com.minpaku.client.render.BodyRenderer;
import com.minpaku.client.render.TaskHighlight;
import com.minpaku.client.screen.BreakerScreen;
import com.minpaku.client.screen.MeetingScreen;
import com.minpaku.client.screen.NumbersScreen;
import com.minpaku.client.screen.ResultScreen;
import com.minpaku.client.screen.RevealScreen;
import com.minpaku.client.screen.SabotageScreen;
import com.minpaku.client.screen.SettingsScreen;
import com.minpaku.client.screen.WiresScreen;
import com.minpaku.game.MarkerType;
import com.minpaku.game.Role;
import com.minpaku.item.ClientBridge;
import com.minpaku.network.Packets;
import com.minpaku.registry.ModEntities;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

public class MinpakuClient implements ClientModInitializer {
    private static boolean wasLeft;
    private static boolean wasRight;
    private static boolean wasSneak;
    private static boolean wasUse;

    @Override
    public void onInitializeClient() {
        EntityRendererRegistry.register(ModEntities.BODY, BodyRenderer::new);
        com.minpaku.client.model.MeshCache.register();
        net.minecraft.client.renderer.blockentity.BlockEntityRenderers.register(
                com.minpaku.registry.ModBlockEntities.MARKER, com.minpaku.client.render.MarkerRenderer::new);
        registerItemModels();
        EntityRendererRegistry.register(ModEntities.BOT, com.minpaku.client.render.BotRenderer::new);
        HudRenderCallback.EVENT.register(GameHud::render);
        GameHud.LEDGER_KEY = net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper.registerKeyBinding(
                new net.minecraft.client.KeyMapping("key.minpaku.ledger", org.lwjgl.glfw.GLFW.GLFW_KEY_J, "key.categories.minpaku"));
        GameHud.KILL_KEY = net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper.registerKeyBinding(
                new net.minecraft.client.KeyMapping("key.minpaku.kill", org.lwjgl.glfw.GLFW.GLFW_KEY_R, "key.categories.minpaku"));
        WorldRenderEvents.AFTER_ENTITIES.register(TaskHighlight::render);

        ClientBridge.openSettings = () -> Minecraft.getInstance().setScreen(new SettingsScreen());
        ClientBridge.openSabotage = () -> {
            Role role = ClientGame.role();
            Minecraft mc = Minecraft.getInstance();
            if (ClientGame.inGame() && role != null && role.canSabotage()) {
                mc.setScreen(new SabotageScreen());
            } else if (mc.player != null) {
                mc.player.displayClientMessage(Component.literal("ゲーム中の人狼だけが使える"), true);
            }
        };

        registerReceivers();

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> client.execute(ClientGame::reset));
        ClientTickEvents.END_CLIENT_TICK.register(MinpakuClient::tick);
    }

    /** ブロックとアイテムの手持ち・インベントリ表示も Blender の3Dモデルで描く */
    private static void registerItemModels() {
        for (var entry : com.minpaku.registry.ModBlocks.BLOCKS.entrySet()) {
            com.minpaku.game.MarkerType type = entry.getKey();
            net.fabricmc.fabric.api.client.rendering.v1.BuiltinItemRendererRegistry.INSTANCE.register(entry.getValue().asItem(),
                    (stack, mode, ps, buffers, light, overlay) -> {
                        // 家具は1ブロックより大きいので、手持ち・インベントリでは少し小さく
                        ps.pushPose();
                        float s = type == com.minpaku.game.MarkerType.BELL || type == com.minpaku.game.MarkerType.SHOJI
                                || type == com.minpaku.game.MarkerType.LAUNDRY || type == com.minpaku.game.MarkerType.BREAKER
                                || type == com.minpaku.game.MarkerType.BOILER || type == com.minpaku.game.MarkerType.WIRING ? 0.55f : 0.72f;
                        ps.translate(0.5, 0.0, 0.5);
                        ps.scale(s, s, s);
                        ps.translate(-0.5, 0.0, -0.5);
                        com.minpaku.client.model.FurnitureArt.draw(type, com.minpaku.client.model.FurnitureArt.Anim.IDLE, ps, buffers,
                                light);
                        ps.popPose();
                    });
        }
        itemModel(com.minpaku.registry.ModItems.KEY, "manager_key");
        itemModel(com.minpaku.registry.ModItems.CRYSTAL, "seer_crystal");
        itemModel(com.minpaku.registry.ModItems.CHARM, "knight_charm");
        itemModel(com.minpaku.registry.ModItems.TALISMAN, "sabotage_talisman");
        itemModel(com.minpaku.registry.ModItems.LENS, "detective_lens");
        itemModel(com.minpaku.registry.ModItems.KEYRING, "okami_keyring");
        itemModel(com.minpaku.registry.ModItems.ONIGIRI, "itamae_onigiri");
    }

    private static void itemModel(net.minecraft.world.item.Item item, String mesh) {
        net.fabricmc.fabric.api.client.rendering.v1.BuiltinItemRendererRegistry.INSTANCE.register(item,
                (stack, mode, ps, buffers, light, overlay) -> {
                    com.minpaku.client.model.StaticMesh m = com.minpaku.client.model.MeshCache.get(mesh);
                    if (m == null) {
                        return;
                    }
                    ps.pushPose();
                    ps.translate(0.5, 0.0, 0.5);
                    m.render(ps, buffers, light, overlay);
                    ps.popPose();
                });
    }

    private static Packets.Meeting pendingMeeting;

    private static void registerReceivers() {
        ClientPlayNetworking.registerGlobalReceiver(Packets.State.TYPE, (p, ctx) -> ctx.client().execute(() -> {
            int before = ClientGame.state.phase();
            ClientGame.onState(p);
            Minecraft mc = Minecraft.getInstance();
            // 会議が終わったのに画面が残っていたら閉じる
            if (before == 2 && p.phase() == 1 && mc.screen instanceof MeetingScreen) {
                mc.setScreen(null);
            }
            if (p.phase() == 0 && (mc.screen instanceof MeetingScreen || mc.screen instanceof RevealScreen)) {
                mc.setScreen(null);
            }
        }));
        ClientPlayNetworking.registerGlobalReceiver(Packets.Reveal.TYPE, (p, ctx) -> ctx.client().execute(() ->
                Minecraft.getInstance().setScreen(new RevealScreen(p))));
        ClientPlayNetworking.registerGlobalReceiver(Packets.Settings.TYPE, (p, ctx) -> ctx.client().execute(() -> {
            ClientGame.settings = p;
            if (Minecraft.getInstance().screen instanceof SettingsScreen s) {
                s.refresh();
            }
        }));
        ClientPlayNetworking.registerGlobalReceiver(Packets.Meeting.TYPE, (p, ctx) -> ctx.client().execute(() -> {
            ClientGame.talk.clear();
            if (ClientGame.killFxKind == 0 && ClientGame.killFxTicks > 0) {
                // 襲われた演出の途中なら、終わってから会議画面を開く
                pendingMeeting = p;
            } else {
                Minecraft.getInstance().setScreen(new MeetingScreen(p));
            }
        }));
        ClientPlayNetworking.registerGlobalReceiver(Packets.Voted.TYPE, (p, ctx) -> ctx.client().execute(() -> {
            if (Minecraft.getInstance().screen instanceof MeetingScreen m) {
                m.onVoted(p.voters());
            }
        }));
        ClientPlayNetworking.registerGlobalReceiver(Packets.MeetingResult.TYPE, (p, ctx) -> ctx.client().execute(() -> {
            if (Minecraft.getInstance().screen instanceof MeetingScreen m) {
                m.onResult(p);
            }
        }));
        ClientPlayNetworking.registerGlobalReceiver(Packets.Preview.TYPE, (p, ctx) -> ctx.client().execute(() ->
                Minecraft.getInstance().setScreen(new com.minpaku.client.screen.PreviewScreen(
                        new com.minpaku.client.scene.EjectScene.Cast(p.kind() == 2 ? Packets.NONE : p.ejected(), p.name(), p.text(),
                                p.carriers())))));
        ClientPlayNetworking.registerGlobalReceiver(Packets.PreviewAnim.TYPE, (p, ctx) -> ctx.client().execute(() ->
                Previews.play(p)));
        ClientPlayNetworking.registerGlobalReceiver(Packets.RoomCounts.TYPE, (p, ctx) -> ctx.client().execute(() -> {
            ClientGame.roomNames = p.rooms();
            ClientGame.roomCounts = p.counts();
        }));
        ClientPlayNetworking.registerGlobalReceiver(Packets.KillFx.TYPE, (p, ctx) -> ctx.client().execute(() -> {
            if (p.kind() == 0) {
                ClientGame.startVictimFx(p.other(), p.otherId(), p.finisher(), p.variant());
            } else {
                ClientGame.killFxKind = p.kind();
                ClientGame.killFxOther = p.other();
                ClientGame.killFxOtherId = p.otherId();
                ClientGame.killFxVariant = p.variant();
                ClientGame.killFxLen = ClientGame.KILL_FX_LEN;
                ClientGame.killFxTicks = ClientGame.killFxLen;
            }
            Minecraft mc = Minecraft.getInstance();
            if (mc.screen != null && !(mc.screen instanceof MeetingScreen)) {
                mc.setScreen(null);
            }
            if (p.kind() == 0) {
                // 音はアニメーションに合わせて ClientGame.tick で鳴らす
            } else if (p.kind() == 1) {
                ClientGame.play(SoundEvents.PLAYER_ATTACK_STRONG, 0.6f);
            } else {
                ClientGame.play(SoundEvents.SHIELD_BLOCK, 1.0f);
            }
        }));
        ClientPlayNetworking.registerGlobalReceiver(Packets.OpenTask.TYPE, (p, ctx) -> ctx.client().execute(() -> {
            Minecraft mc = Minecraft.getInstance();
            MarkerType type = MarkerType.byOrdinal(p.kind());
            switch (type) {
                case WIRING -> mc.setScreen(new WiresScreen(p.pos()));
                case LEDGER -> mc.setScreen(new NumbersScreen(p.pos()));
                case BREAKER -> mc.setScreen(new BreakerScreen(p.pos()));
                case SHOJI -> mc.setScreen(new com.minpaku.client.screen.HolesScreen(p.pos()));
                default -> {
                }
            }
        }));
        ClientPlayNetworking.registerGlobalReceiver(Packets.Announce.TYPE, (p, ctx) -> ctx.client().execute(() -> {
            ClientGame.announce(p.title(), p.subtitle(), p.color());
            switch (p.sound()) {
                case 1 -> {
                    ClientGame.play(SoundEvents.RAID_HORN, 1.0f);
                    ClientGame.play(SoundEvents.BELL_BLOCK, 0.5f);
                }
                case 2 -> ClientGame.play(SoundEvents.BELL_BLOCK, 1.0f);
                case 3 -> ClientGame.play(SoundEvents.AMETHYST_BLOCK_CHIME, 1.0f);
                default -> {
                }
            }
        }));
        ClientPlayNetworking.registerGlobalReceiver(Packets.Talk.TYPE, (p, ctx) -> ctx.client().execute(() -> {
            ClientGame.talk.add(p);
            if (ClientGame.talk.size() > 60) {
                ClientGame.talk.remove(0);
            }
        }));
        ClientPlayNetworking.registerGlobalReceiver(Packets.Result.TYPE, (p, ctx) -> ctx.client().execute(() ->
                Minecraft.getInstance().setScreen(new ResultScreen(p))));
    }

    private static void tick(Minecraft mc) {
        if (pendingMeeting != null && (ClientGame.killFxTicks <= 0 || ClientGame.killFxKind != 0)) {
            if (ClientGame.state.phase() == 2) {
                mc.setScreen(new MeetingScreen(pendingMeeting));
            }
            pendingMeeting = null;
        }
        ClientGame.tick();
        Previews.tick();
        SelfTest.tick(mc);
        // 人狼の能力：キルキーで近くの相手をキル
        while (GameHud.KILL_KEY != null && GameHud.KILL_KEY.consumeClick()) {
            LivingEntity target = KillTarget.find(mc);
            if (target != null && mc.screen == null) {
                ClientPlayNetworking.send(new Packets.KillRequest(target.getUUID()));
            }
        }
        if (mc.player == null || !ClientGame.inGame()) {
            return;
        }
        Packets.State s = ClientGame.state;

        // ベントの中：A/D で移動、スニークで出る
        if (s.inVent() && mc.screen == null) {
            boolean left = mc.options.keyLeft.isDown();
            boolean right = mc.options.keyRight.isDown();
            boolean sneak = mc.options.keyShift.isDown();
            if (left && !wasLeft) {
                ClientPlayNetworking.send(new Packets.VentMove(-1));
            }
            if (right && !wasRight) {
                ClientPlayNetworking.send(new Packets.VentMove(1));
            }
            if (sneak && !wasSneak) {
                ClientPlayNetworking.send(new Packets.VentMove(0));
            }
            wasLeft = left;
            wasRight = right;
            wasSneak = sneak;
        } else {
            wasLeft = wasRight = wasSneak = false;
        }

        // 幽霊（スペクテイター）の右クリックでタスク
        if (!s.alive() && !s.inVent() && s.phase() == 1 && mc.screen == null && mc.player.isSpectator()) {
            boolean use = mc.options.keyUse.isDown();
            if (use && !wasUse) {
                HitResult hit = mc.player.pick(5.0, 1f, false);
                if (hit instanceof BlockHitResult bh && hit.getType() == HitResult.Type.BLOCK) {
                    ClientPlayNetworking.send(new Packets.GhostUse(bh.getBlockPos()));
                }
            }
            wasUse = use;
        } else {
            wasUse = false;
        }
    }
}
