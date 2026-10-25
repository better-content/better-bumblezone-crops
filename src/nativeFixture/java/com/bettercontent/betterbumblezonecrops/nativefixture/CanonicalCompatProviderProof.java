package com.bettercontent.betterbumblezonecrops.nativefixture;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import java.util.zip.ZipFile;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.ForgeRegistries;

/** Native-only canonical dependency receipt; actual loaded metadata and stateless behavior, not a copied repair. */
public final class CanonicalCompatProviderProof {
    private static final String PREFIX = "bc.crops.providers.";
    private static final List<String> IDS = List.of("better_compat_fixes", "better_gameplay_notices");
    private static final List<String> REPOSITORIES = List.of("better-compat-fixes", "better-gameplay-notices");
    private static final List<String> ARTIFACTS = List.of("better-compat-fixes-0.1.9.jar", "better-gameplay-notices-1.0.0.jar");
    private CanonicalCompatProviderProof() {}
    private static void require(boolean value, String message) {
        if (!value) throw new IllegalStateException(message);
    }
    private static String sha(byte[] bytes) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
    }
    private static Path ordinary(Path path) {
        path = path.toAbsolutePath().normalize();
        for (Path part = path; part != null; part = part.getParent())
            require(!Files.isSymbolicLink(part), "Symlink canonical provider input");
        require(Files.isRegularFile(path), "Missing canonical provider input");
        return path;
    }
    public static JsonObject observe(MinecraftServer server) throws Exception {
        // StructureUtils imports packed SNBT with NbtUtils, not TagParser alone.
        // Binary-NBT-shaped palette compounds are silently discarded by that importer.
        String templateText = Files.readString(ordinary(Path.of("gameteststructures/flower_lit.snbt")));
        var authored = net.minecraft.nbt.NbtUtils.snbtToStructure(templateText);
        var palette = authored.getList("palette", 10);
        var blocks = authored.getList("blocks", 10);
        require(palette.size() == 1 && "minecraft:glowstone".equals(palette.getCompound(0).getString("Name")) &&
            blocks.size() == 1 && blocks.getCompound(0).getInt("state") == 0,
            "Actual native SNBT importer lost authored flower lamp");
        var lamp = blocks.getCompound(0).getList("pos", 3);
        require(lamp.size() == 3 && lamp.getInt(0) == 3 && lamp.getInt(1) == 0 && lamp.getInt(2) == 2,
            "Authored template lamp coordinates differ from native structure offset");
        var binaryShape = net.minecraft.nbt.NbtUtils.snbtToStructure(
            "{size:[5,5,5],entities:[],blocks:[{pos:[3,0,2],state:0}],palette:[{Name:\"minecraft:glowstone\"}]}");
        require(binaryShape.getList("palette", 10).isEmpty(), "Native SNBT binary-shape negative control drifted");
        require(server.overworld().getChunkSource().getGenerator() instanceof net.minecraft.world.level.levelgen.FlatLevelSource,
                "Authored crop fixture requires actual flat isolation, not unrelated normal terrain generation");
        Path receipt = ordinary(Path.of(System.getProperty(PREFIX + "binding")));
        byte[] bytes = Files.readAllBytes(receipt);
        require(bytes.length <= 64 * 1024 && sha(bytes).equals(System.getProperty(PREFIX + "binding.sha256")),
            "Canonical provider receipt changed");
        var input = JsonParser.parseString(new String(bytes, java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();
        require("bc.crops.canonical_providers.v1".equals(input.get("schema").getAsString()) &&
            input.get("attempt").getAsString().equals(System.getProperty(PREFIX + "attempt")) &&
            receipt.getParent().getFileName().toString().equals(input.get("attempt").getAsString()),
            "Canonical provider attempt differs");
        var rows = input.getAsJsonArray("providers");
        require(rows.size() == IDS.size(), "Canonical provider population differs");
        var observed = new JsonArray();
        for (int i = 0; i < rows.size(); i++) {
            var row = rows.get(i).getAsJsonObject();
            String id = row.get("mod_id").getAsString();
            Path artifact = ordinary(Path.of(row.get("path").getAsString()));
            require(IDS.get(i).equals(id) && ARTIFACTS.get(i).equals(row.get("artifact").getAsString()) &&
                artifact.getFileName().toString().equals(ARTIFACTS.get(i)) &&
                sha(Files.readAllBytes(artifact)).equals(row.get("sha256").getAsString()),
                "Canonical provider identity or bytes differ");
            byte[] metadata;
            try (var zip = new ZipFile(artifact.toFile());
                 var stream = zip.getInputStream(zip.getEntry("META-INF/better-content-source.properties"))) {
                metadata = stream.readAllBytes();
            }
            require(sha(metadata).equals(row.get("source_metadata_sha256").getAsString()), "Canonical source metadata changed");
            var properties = new java.util.Properties();
            properties.load(new java.io.ByteArrayInputStream(metadata));
            require("bc.custom_mod_source.v1".equals(properties.getProperty("schema")) &&
                id.equals(properties.getProperty("mod_id")) &&
                REPOSITORIES.get(i).equals(properties.getProperty("repository")), "Canonical source identity fields differ");
            var loaded = ModList.get().getModFileById(id);
            require(loaded != null && ModList.get().isLoaded(id), "Canonical provider not actually loaded");
            var loadedMetadata = loaded.getFile().findResource("META-INF", "better-content-source.properties");
            require(java.util.Arrays.equals(metadata, Files.readAllBytes(loadedMetadata)),
                "Loaded mapped provider source identity differs from canonical artifact");
            var item = row.deepCopy();
            item.addProperty("loadedSourceMetadataMatches", true);
            observed.add(item);
        }
        var id = new ResourceLocation("supplementaries", "urn");
        var urn = ForgeRegistries.BLOCKS.getValue(id);
        require(urn != null && id.equals(ForgeRegistries.BLOCKS.getKey(urn)), "Required native urn absent");
        var property = urn.getStateDefinition().getProperty("treasure");
        require(property instanceof BooleanProperty, "Pinned native urn property differs");
        var treasure = (BooleanProperty) property;
        var treasureState = urn.defaultBlockState().setValue(treasure, true);
        var ordinaryState = urn.defaultBlockState().setValue(treasure, false);
        require(treasureState.hasBlockEntity() && ordinaryState.hasBlockEntity() && urn instanceof EntityBlock,
            "Original global urn block-entity predicate must remain unchanged");
        var level = server.overworld();
        var pos = new BlockPos(0, level.getMaxBuildHeight() - 8, 0);
        // A genuine native LevelChunk, never registered in the world's cache. Its pending marker
        // stays intact for the whole control; no cleanup can purge it or manufacture a tile.
        var chunk = new LevelChunk(level, new ChunkPos(pos));
        var original = chunk.getBlockState(pos);
        require(original.isAir() && !chunk.getBlockEntities().containsKey(pos) && chunk.getBlockEntityNbt(pos) == null,
            "Native provider control requires an empty high-altitude cell; no authored state may be displaced");
        require(((EntityBlock) urn).newBlockEntity(pos, treasureState) == null &&
            ((EntityBlock) urn).newBlockEntity(pos, ordinaryState) != null,
            "Native treasure/ordinary factory contract differs");
        var marker = new CompoundTag();
        marker.putString("id", "DUMMY");
        marker.putInt("x", pos.getX()); marker.putInt("y", pos.getY()); marker.putInt("z", pos.getZ());
        // Prepare only this chunk's owned native palette/counters. LevelChunk.setBlockState
        // would enqueue real-level lighting work and invoke world callbacks even when unregistered.
        var section = chunk.getSection(level.getSectionIndex(pos.getY()));
        require(section.setBlockState(pos.getX() & 15, pos.getY() & 15, pos.getZ() & 15, treasureState) == original,
            "Native provider owned-section setup failed");
        chunk.setBlockEntityNbt(marker);
        require(chunk.getBlockEntity(pos, LevelChunk.EntityCreationType.CHECK) == null &&
            marker.equals(chunk.getBlockEntityNbt(pos)) && !chunk.getBlockEntities().containsKey(pos),
            "Actual loaded production lookup repair did not preserve the synthetic marker without a tile");
        var result = new JsonObject();
        result.addProperty("schema", "bc.crops.canonical_providers.v1");
        result.addProperty("attempt", input.get("attempt").getAsString());
        result.addProperty("bindingSha256", sha(bytes));
        result.addProperty("treasureUrnStatelessRepairObserved", true);
        result.addProperty("originalGlobalHasBlockEntityPreserved", true);
        result.addProperty("actualLookupRetainsSyntheticMarker", true);
        result.addProperty("controlScope", "fresh_unregistered_level_chunk");
        result.addProperty("authoredFlowerTemplateImported", true);
        result.addProperty("nativeBinaryShapeNegativeControlObserved", true);
        result.addProperty("authoredFlowerTemplateSha256", sha(templateText.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        result.add("providers", observed);
        return result;
    }
}
