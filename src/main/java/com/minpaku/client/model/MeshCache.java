package com.minpaku.client.model;

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
 * 3Dモデルの読み込み。
 *   1) .minecraft/config/minpaku/mesh/<名前>.json があればそれ（Blender で直したものを試す用）
 *   2) なければ mod の中の assets/minpaku/mesh/<名前>.json
 * F3+T で読み直す。
 */
public final class MeshCache {
    private static final Map<String, Optional<StaticMesh>> CACHE = new HashMap<>();

    private MeshCache() {
    }

    public static void register() {
        ResourceManagerHelper.get(PackType.CLIENT_RESOURCES).registerReloadListener(new SimpleSynchronousResourceReloadListener() {
            @Override
            public ResourceLocation getFabricId() {
                return MinpakuJinrou.id("mesh_cache");
            }

            @Override
            public void onResourceManagerReload(ResourceManager manager) {
                CACHE.clear();
            }
        });
    }

    @Nullable
    public static StaticMesh get(String name) {
        return CACHE.computeIfAbsent(name, MeshCache::load).orElse(null);
    }

    private static Optional<StaticMesh> load(String name) {
        try {
            JsonObject json = null;
            Path file = FabricLoader.getInstance().getConfigDir().resolve("minpaku/mesh/" + name + ".json");
            if (Files.isRegularFile(file)) {
                try (Reader r = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                    json = JsonParser.parseReader(r).getAsJsonObject();
                }
            }
            if (json == null) {
                Optional<Resource> res = Minecraft.getInstance().getResourceManager()
                        .getResource(MinpakuJinrou.id("mesh/" + name + ".json"));
                if (res.isEmpty()) {
                    return Optional.empty();
                }
                try (Reader r = res.get().openAsReader()) {
                    json = JsonParser.parseReader(r).getAsJsonObject();
                }
            }
            return Optional.of(StaticMesh.parse(json));
        } catch (Exception e) {
            MinpakuJinrou.LOGGER.error("3Dモデル {} を読み込めませんでした", name, e);
            return Optional.empty();
        }
    }
}
