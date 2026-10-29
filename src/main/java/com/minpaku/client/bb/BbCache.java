package com.minpaku.client.bb;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.minpaku.MinpakuJinrou;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import org.jetbrains.annotations.Nullable;

import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * フィニッシャーの Blockbench プロジェクトの読み込み。
 *   1) .minecraft/config/minpaku/finishers/<名前>.bbmodel があればそれ（Blockbench で直したものを試す用）
 *   2) なければ mod の中の assets/minpaku/finishers/<名前>.bbmodel
 * F3+T で読み直す。
 */
public final class BbCache {
    private static final Map<String, Optional<BbModel>> CACHE = new HashMap<>();

    private BbCache() {
    }

    public static void register() {
        ResourceManagerHelper.get(PackType.CLIENT_RESOURCES).registerReloadListener(new SimpleSynchronousResourceReloadListener() {
            @Override
            public ResourceLocation getFabricId() {
                return MinpakuJinrou.id("finisher_cache");
            }

            @Override
            public void onResourceManagerReload(ResourceManager manager) {
                CACHE.clear();
            }
        });
    }

    @Nullable
    public static BbModel get(String name) {
        return CACHE.computeIfAbsent(name, BbCache::load).orElse(null);
    }

    private static Optional<BbModel> load(String name) {
        try {
            JsonObject json = null;
            Path file = FabricLoader.getInstance().getConfigDir().resolve("minpaku/finishers/" + name + ".bbmodel");
            if (Files.isRegularFile(file)) {
                try (Reader r = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                    json = JsonParser.parseReader(r).getAsJsonObject();
                }
            }
            if (json == null) {
                Optional<Resource> res = Minecraft.getInstance().getResourceManager()
                        .getResource(MinpakuJinrou.id("finishers/" + name + ".bbmodel"));
                if (res.isEmpty()) {
                    return Optional.empty();
                }
                try (Reader r = res.get().openAsReader()) {
                    json = JsonParser.parseReader(r).getAsJsonObject();
                }
            }
            return Optional.of(BbModel.parse(json));
        } catch (Exception e) {
            MinpakuJinrou.LOGGER.error("フィニッシャー {} を読み込めませんでした", name, e);
            return Optional.empty();
        }
    }
}
