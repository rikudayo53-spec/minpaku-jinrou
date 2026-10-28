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
        HudRenderCallback.EVENT.register(GameHud::render);
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

    private static void registerReceivers() {
        ClientPlayNetworking.registerGlobalReceiver(Packets.State.TYPE, (p, ctx) -> ctx.client().execute(() -> {
            int before = ClientGame.state.phase();
            ClientGame.state = p;
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
        ClientPlayNetworking.registerGlobalReceiver(Packets.Meeting.TYPE, (p, ctx) -> ctx.client().execute(() ->
                Minecraft.getInstance().setScreen(new MeetingScreen(p))));
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
        ClientPlayNetworking.registerGlobalReceiver(Packets.KillFx.TYPE, (p, ctx) -> ctx.client().execute(() -> {
            ClientGame.killFxKind = p.kind();
            ClientGame.killFxOther = p.other();
            ClientGame.killFxTicks = ClientGame.KILL_FX_LEN;
            Minecraft mc = Minecraft.getInstance();
            if (mc.screen != null && !(mc.screen instanceof MeetingScreen)) {
                mc.setScreen(null);
            }
            if (p.kind() == 0) {
                ClientGame.play(SoundEvents.PLAYER_HURT, 0.5f);
                ClientGame.play(SoundEvents.WITHER_HURT, 0.6f);
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
        ClientPlayNetworking.registerGlobalReceiver(Packets.Result.TYPE, (p, ctx) -> ctx.client().execute(() ->
                Minecraft.getInstance().setScreen(new ResultScreen(p))));
    }

    private static void tick(Minecraft mc) {
        ClientGame.tick();
        SelfTest.tick(mc);
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
