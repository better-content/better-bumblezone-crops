package com.bettercontent.betterbumblezonecrops.nativefixture;

import com.google.gson.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.PathPackResources;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraftforge.event.AddPackFindersEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;

/** Required gameTestServer-only data: ten exact Root resources + separately labelled fixed72 TRANSFORMED index. */
@Mod.EventBusSubscriber(modid = "better_bumblezone_crops", bus = Mod.EventBusSubscriber.Bus.MOD)
public final class CanonicalDataFixture {
    private static final String PROPERTY = "bc.crops.canonical.manifest";
    private static final String PACK_ID = CanonicalPackOrder.PACK_ID;
    private static final int MAX_BYTES = 2 * 1024 * 1024;
    private CanonicalDataFixture() {}
    private static void require(boolean valid, String message) {
        if (!valid) throw new IllegalStateException(message);
    }
    private static String sha(byte[] bytes) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
    }
    /** Failure-only scalar context; never an origin identity or acceptance alias. */
    private static String originScalar(String value) {
        if (value == null) return "UNKNOWN(no_observation)";
        var escaped = new StringBuilder();
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            boolean plain = c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z'
                || c >= '0' && c <= '9' || "._:/-".indexOf(c) >= 0;
            escaped.append(plain ? String.valueOf(c) : "\\u" + HexFormat.of().toHexDigits(c));
            if (escaped.length() > 128) {
                String digest;
                try { digest = sha(value.getBytes(java.nio.charset.StandardCharsets.UTF_8)); }
                catch (Exception unavailable) { digest = "UNKNOWN(hash_unavailable)"; }
                return "OVERFLOW(chars=" + value.length() + ",sha256=" + digest + ")";
            }
        }
        return "\"" + escaped + "\"";
    }
    private static String originFailureContext(int ordinal, String id, String resource, String expected,
            String actualLastCompared, String attempt, int apply) {
        // Six <=130-character scalar fields (or shorter length/hash evidence) keep this below2048.
        return "In-scope actual resource data-owner origin differs; ordinal=" + ordinal
            + "; id=" + originScalar(id) + "; resource=" + originScalar(resource)
            + "; expected=" + originScalar(expected) + "; actual_last_compared=" + originScalar(actualLastCompared)
            + "; attempt=" + originScalar(attempt) + "; apply=" + apply;
    }
    private static byte[] read(Path path) throws Exception {
        for (var p = path; p != null; p = p.getParent()) require(!Files.isSymbolicLink(p), "Symlink canonical input");
        try (var stream = Files.newInputStream(path)) {
            var bytes = stream.readNBytes(MAX_BYTES + 1);
            require(bytes.length <= MAX_BYTES, "Oversize canonical input");
            return bytes;
        }
    }
    private static JsonObject manifest() throws Exception {
        String property = System.getProperty(PROPERTY);
        require(property != null, "Explicit canonical manifest missing");
        Path path = Path.of(property).toAbsolutePath();
        byte[] bytes = read(path);
        require(sha(bytes).equals(System.getProperty(PROPERTY + ".sha256")), "Canonical manifest hash drift");
        var data = JsonParser.parseString(new String(bytes, java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();
        require(data.get("schema").getAsString().equals("bc.crops_canonical_snapshot.v2"), "Canonical manifest schema");
        var projection = data.getAsJsonObject("projection");
        require(projection != null && projection.has("owner_projection_version")
            && "bc.crops.logical_data_owner.v1".equals(projection.get("owner_projection_version").getAsString()),
            "Unsupported logical owner projection; old guessed pack labels are not admitted");
        require(data.get("global_index_projected").getAsBoolean(), "Required transformed fixed policy missing");
        require(data.get("attempt").getAsString().equals(System.getProperty(PROPERTY + ".attempt")), "Canonical attempt drift");
        require(path.getParent().getFileName().toString().equals(data.get("attempt").getAsString()), "Canonical token path drift");
        require(data.get("pack_id").getAsString().equals(PACK_ID) && data.get("pack_format").getAsInt() == 15, "Canonical pack identity/format drift");
        try (var input = CanonicalDataFixture.class.getResourceAsStream("/bc_crops_canonical_source_scope.json")) {
            require(input != null, "Native fixture scope missing");
            byte[] scopeBytes = input.readNBytes(MAX_BYTES + 1);
            require(scopeBytes.length <= MAX_BYTES && sha(scopeBytes).equals(data.get("scope_sha256").getAsString()), "Native fixture scope drift");
            var scope = JsonParser.parseString(new String(scopeBytes, java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();
            var expected = scope.getAsJsonArray("resources");
            var rows = data.getAsJsonArray("resources");
            require(expected.size() == 10 && rows.size() == expected.size(), "Canonical resource coverage drift");
            Set<String> unique = new HashSet<>();
            for (int i = 0; i < rows.size(); i++) {
                String resource = rows.get(i).getAsJsonObject().get("resource").getAsString();
                require(resource.equals(expected.get(i).getAsString()) && unique.add(resource), "Canonical resource order/identity drift");
                require(resource.startsWith("data/") && !resource.contains("..") && !resource.equals("data/forge/loot_modifiers/global_loot_modifiers.json"), "Forbidden source resource");
            }
        }
        return data;
    }
    @SubscribeEvent
    public static void find(AddPackFindersEvent event) {
        if (event.getPackType() != PackType.SERVER_DATA) return;
        try {
            manifest();
            Path pack = Path.of(System.getProperty(PROPERTY)).toAbsolutePath().getParent().resolve("pack");
            event.addRepositorySource(consumer -> consumer.accept(Pack.create(PACK_ID, Component.literal(PACK_ID), true,
                id -> new PathPackResources(id, pack, false),
                new Pack.Info(Component.literal(PACK_ID), 15, FeatureFlagSet.of()),
                PackType.SERVER_DATA, Pack.Position.TOP, true, PackSource.BUILT_IN)));
        } catch (Exception e) { throw new IllegalStateException("Canonical source pack admission failed", e); }
    }
    private static final java.util.List<String> RELOAD_SCALAR_PROOFS = new java.util.ArrayList<>();
    private static int completedApplyReturns;
    private static boolean published;
    private static String scalarFailure;

    static List<String> codecProofIds() throws Exception {
        var ids = manifest().getAsJsonObject("projection").getAsJsonArray("ordered_ids");
        List<String> result = new ArrayList<>();
        for (var id : ids) result.add(id.getAsString());
        return result;
    }
    /** Existing selected logical owner -> existing registered pack. No effective Resource input. */
    private static JsonArray expectedPackIds(JsonObject row, Map<String, String> ownersByPackId) {
        require(!row.has("expected_source_pack_ids") && row.has("expected_owner"), "Old/combined guessed owner representation");
        var owner = row.getAsJsonObject("expected_owner");
        String kind = owner.get("kind").getAsString();
        String group = owner.get("group").getAsString();
        String origin = row.get("origin").getAsString();
        var result = new JsonArray();
        if (kind.equals("canonical_factory")) {
            require(owner.keySet().equals(Set.of("kind", "group", "pack_id")) && origin.startsWith("root://")
                && group.equals(PACK_ID) && owner.get("pack_id").getAsString().equals(PACK_ID), "Canonical owner factory differs");
            claimOwnerPack(ownersByPackId, PACK_ID, group);
            result.add(PACK_ID);
            return result;
        }
        require(owner.keySet().equals(Set.of("kind", "group", "owner_mod_ids")), "Malformed logical owner descriptor");
        require(kind.equals("archive_mod") && origin.startsWith("archive://") && group.equals(origin)
            || kind.equals("source_mod") && origin.startsWith("source://") && group.equals("source://better_bumblezone_crops"),
            "Selected logical owner group differs");
        var keys = owner.getAsJsonArray("owner_mod_ids");
        require(keys.size() > 0 && keys.size() <= 72, "Missing/overflow logical owner keys");
        Set<String> declared = new HashSet<>();
        for (var key : keys) {
            String id = key.getAsString();
            require(!id.isBlank() && id.length() <= 64 && declared.add(id), "Malformed/ambiguous logical owner key");
        }
        require(!kind.equals("source_mod") || declared.equals(Set.of("better_bumblezone_crops")), "Own source group differs");
        net.minecraftforge.forgespi.locating.IModFile selectedFile = null;
        net.minecraftforge.resource.PathPackResources selectedPack = null;
        for (var key : keys) {
            String id = key.getAsString();
            var info = net.minecraftforge.fml.ModList.get().getModFileById(id);
            require(info != null, "Selected logical owner missing");
            var mods = info.getMods();
            require(mods.size() == declared.size(), "Selected logical owner metadata count differs");
            Set<String> actualGroup = new HashSet<>();
            for (var mod : mods) require(actualGroup.add(mod.getModId()), "Ambiguous selected mod metadata");
            require(actualGroup.equals(declared), "Differently selected logical owner metadata group");
            var file = info.getFile();
            var pack = net.minecraftforge.resource.ResourcePackLoader.getPackFor(id).orElseThrow(
                () -> new IllegalStateException("Selected logical owner registered pack missing"));
            require(file != null && (selectedFile == null || selectedFile == file)
                && (selectedPack == null || selectedPack == pack), "Logical owner keys select different files/packs");
            String packId = pack.packId();
            require(packId != null && !packId.isBlank() && packId.equals(file.getFileName()), "Registered owner constructor identity differs");
            selectedFile = file;
            selectedPack = pack;
            claimOwnerPack(ownersByPackId, packId, group);
            result.add(packId); // Preserve one candidate per declared key, including equal registered IDs.
        }
        return result;
    }
    /** Per existing72 traversal only; scalar state never contains loader/pack/Native objects. */
    private static void claimOwnerPack(Map<String, String> ownersByPackId, String packId, String group) {
        if (ownersByPackId == null) return; // Original Ars input check; full collision proof stays at manager RETURN.
        String previous = ownersByPackId.putIfAbsent(packId, group);
        require(previous == null || previous.equals(group), "Distinct selected logical owners share a pack ID");
        require(ownersByPackId.size() <= 73, "Logical owner collision proof overflow");
    }
    static void verifyOriginalArsInput(net.minecraft.server.packs.resources.ResourceManager resources,
            JsonElement original) throws Exception {
        JsonObject expected = null;
        for (var item : manifest().getAsJsonObject("projection").getAsJsonArray("resources")) {
            var row = item.getAsJsonObject();
            if (!ArsDispatchLedger.ID.equals(row.get("id").getAsString())) continue;
            require(expected == null, "Duplicate Ars input row");
            var expectedIds = expectedPackIds(row, null);
            String path = row.get("resource").getAsString();
            int split = path.indexOf('/', 5);
            var resource = resources.getResource(new ResourceLocation(path.substring(5, split), path.substring(split + 1))).orElseThrow();
            boolean origin = false;
            for (var id : expectedIds) origin |= id.getAsString().equals(resource.sourcePackId());
            require(origin, "Original Ars data-owner differs");
            try (var input = resource.open()) {
                byte[] bytes = input.readNBytes(MAX_BYTES + 1);
                require(bytes.length <= MAX_BYTES && sha(bytes).equals(row.get("sha256").getAsString()), "Original Ars definition differs");
                expected = JsonParser.parseString(new String(bytes, java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();
            }
        }
        require(expected != null && expected.equals(original), "Original manager Ars JSON differs from effective admitted definition");
    }
    /** Boundary-local only: native receiver/ResourceManager never enter stored state. */
    public static synchronized void atNativeApplyReturn(net.minecraftforge.common.loot.LootModifierManager manager,
            net.minecraft.server.packs.resources.ResourceManager resources) {
        completedApplyReturns++;
        try {
            require(completedApplyReturns <= 4 && !published, "Reload proof overflow/late capture");
            String scalar = new Gson().toJson(verifyGlm(resources, manager, manifest()));
            require(scalar.getBytes(java.nio.charset.StandardCharsets.UTF_8).length <= 256 * 1024, "Scalar reload proof overflow");
            RELOAD_SCALAR_PROOFS.add(scalar);
        } catch (Exception error) {
            scalarFailure = error.getClass().getSimpleName();
            throw new IllegalStateException("Required native data proof failed at original apply RETURN", error);
        } finally {
            ArsDispatchProof.end();
        }
    }
    private static synchronized JsonObject publishGlmScalars() {
        require(!published && scalarFailure == null && completedApplyReturns > 0 && completedApplyReturns <= 4
            && RELOAD_SCALAR_PROOFS.size() == completedApplyReturns, "Missing/incomplete/failed/duplicate reload proof");
        published = true;
        JsonObject result = JsonParser.parseString(RELOAD_SCALAR_PROOFS.get(RELOAD_SCALAR_PROOFS.size() - 1)).getAsJsonObject();
        var reloads = new JsonArray();
        for (String scalar : RELOAD_SCALAR_PROOFS) reloads.add(JsonParser.parseString(scalar));
        result.addProperty("completedOriginalApplyReturns", completedApplyReturns);
        result.addProperty("allObservedReturnsProved", true);
        result.add("reloadScalarProofs", reloads);
        return result;
    }
    private static JsonObject verifyGlm(net.minecraft.server.packs.resources.ResourceManager resources,
            net.minecraftforge.common.loot.LootModifierManager manager, JsonObject data) throws Exception {
        var projection = data.getAsJsonObject("projection");
        JsonObject fixed;
        try (var input = CanonicalDataFixture.class.getResourceAsStream("/bc_crops_native_glm_policy.json")) {
            require(input != null, "Owned frozen GLM manifest missing");
            byte[] bytes = input.readNBytes(MAX_BYTES + 1);
            require(bytes.length <= MAX_BYTES && sha(bytes).equals(projection.get("frozen_manifest_sha256").getAsString()), "Owned fixed policy drift");
            fixed = JsonParser.parseString(new String(bytes, java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();
        }
        require(fixed.getAsJsonArray("root_rows").size() == 226, "Missing full canonical row/omission proof");
        var ids = fixed.getAsJsonArray("proposed_ordered_in_domain_ids");
        require(ids.size() == 72 && ids.equals(projection.getAsJsonArray("ordered_ids")), "Mandatory fixed72 identity/order drift");
        var indexId = new ResourceLocation("forge", "loot_modifiers/global_loot_modifiers.json");
        var effectiveIndex = resources.getResource(indexId).orElseThrow();
        require(PACK_ID.equals(effectiveIndex.sourcePackId()), "Transformed policy has wrong actual origin");
        JsonObject index;
        try (var input = effectiveIndex.open()) {
            byte[] bytes = input.readNBytes(MAX_BYTES + 1);
            require(bytes.length <= MAX_BYTES && sha(bytes).equals(projection.get("index_sha256").getAsString()), "Effective transformed index bytes differ");
            index = JsonParser.parseString(new String(bytes, java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();
        }
        require(index.get("replace").getAsBoolean() && ids.equals(index.getAsJsonArray("entries")), "Effective transformed membership/order drift");
        var loader = fixed.getAsJsonObject("native_loader_apply");
        require(net.minecraftforge.versions.forge.ForgeVersion.getVersion().equals(loader.get("forge_version").getAsString()), "Actual Forge SDK version differs");
        try (var input = net.minecraftforge.common.loot.LootModifierManager.class.getResourceAsStream("/" + loader.get("class_resource").getAsString())) {
            require(input != null, "Pinned native manager class resource absent");
            byte[] bytes = input.readNBytes(MAX_BYTES + 1);
            require(bytes.length <= MAX_BYTES && sha(bytes).equals(loader.get("mapped_class_sha256").getAsString()), "Pinned native loader bytecode differs");
        }
        require(manager.getClass() == net.minecraftforge.common.loot.LootModifierManager.class, "Unexpected native manager implementation");
        // Actual pinned manager apply uses ordered index traversal -> ImmutableMap.Builder -> values().
        // No private map, reflection, second decode, modifier.apply, loot context or world query.
        var loaded = new ArrayList<>(manager.getAllLootMods());
        var expected = projection.getAsJsonArray("resources");
        require(expected.size() == 72 && loaded.size() == 72, "Mandatory actual decoded72 incomplete/extra");
        var registry = net.minecraftforge.registries.ForgeRegistries.GLOBAL_LOOT_MODIFIER_SERIALIZERS.get();
        require(registry != null, "Actual native codec registry missing");
        var rows = new JsonArray();
        Map<String, String> ownersByPackId = new HashMap<>();
        ownersByPackId.put(PACK_ID, PACK_ID);
        for (int i = 0; i < 72; i++) {
            var row = expected.get(i).getAsJsonObject();
            require(row.get("id").equals(ids.get(i)), "Decoded projection ordinal drift");
            var codecId = new ResourceLocation(row.get("codec").getAsString());
            boolean ars = ArsDispatchLedger.ID.equals(row.get("id").getAsString());
            if (ars) {
                require(ArsDispatchLedger.ID.equals(codecId.toString()), "Ars primary codec differs");
                // Its nonmemoized codec() is deliberately NEVER called. Original dispatch/body witnesses below.
            }
            require(registry.getValue(codecId) != null && codecId.equals(registry.getKey(registry.getValue(codecId))),
                "Actual native codec registry key differs");
            boolean freshAccessor = ars || codecId.toString().equals("goety:add_loot");
            if (!freshAccessor) {
                var actualCodec = loaded.get(i).codec();
                require(actualCodec != null && registry.getValue(codecId) == actualCodec
                    && codecId.equals(registry.getKey(actualCodec)), "Actual memoized codec identity/order differs");
            }
            var expectedIds = expectedPackIds(row, ownersByPackId);
            String path = row.get("resource").getAsString();
            int split = path.indexOf('/', 5);
            var resource = resources.getResource(new ResourceLocation(path.substring(5, split), path.substring(split + 1))).orElseThrow();
            boolean origin = false;
            String actualLastCompared = null;
            for (var candidate : expectedIds) origin |= candidate.getAsString().equals(actualLastCompared = resource.sourcePackId());
            require(origin, origin ? "In-scope actual resource data-owner origin differs" : originFailureContext(i,
                row.get("id").getAsString(), path, expectedIds.toString(),
                actualLastCompared, data.get("attempt").getAsString(), completedApplyReturns));
            try (var input = resource.open()) {
                byte[] bytes = input.readNBytes(MAX_BYTES + 1);
                require(bytes.length <= MAX_BYTES && sha(bytes).equals(row.get("sha256").getAsString()), "Mandatory effective definition absent/stale");
                var canonicalInput = JsonParser.parseString(new String(bytes, java.nio.charset.StandardCharsets.UTF_8));
                OriginalCodecDispatchProof.requireLoaded(i, new ResourceLocation(row.get("id").getAsString()),
                    codecId, canonicalInput, loaded.get(i));
                if (ars) ArsDispatchProof.requireLoaded(i, loaded.get(i), canonicalInput);
            }
            var item = row.deepCopy();
            item.addProperty("ordinal", i);
            item.addProperty("actualCodecRegistryKey", codecId.toString());
            item.addProperty("codecIdentityConfirmed", true);
            item.addProperty("instanceCodecIdentityChecked", !freshAccessor);
            item.addProperty("codecIdentityProofKind", "ORIGINAL_REGISTERED_DISPATCH_AND_ROOT_BODY");
            item.addProperty("effectiveSourcePackId", resource.sourcePackId());
            item.add("expectedRegisteredSourcePackIds", expectedIds.deepCopy());
            rows.add(item);
        }
        var result = new JsonObject();
        result.addProperty("kind", "TRANSFORMED_FIXED72_NOT_BYTE_IDENTICAL_ROOT_INDEX");
        result.addProperty("indexSourcePackId", effectiveIndex.sourcePackId());
        result.addProperty("indexSha256", projection.get("index_sha256").getAsString());
        result.addProperty("canonical226ParentSha256", projection.get("canonical_parent_sha256").getAsString());
        result.addProperty("frozenManifestSha256", projection.get("frozen_manifest_sha256").getAsString());
        result.addProperty("gameplayApplyConditionsOrEffectsPerformedForAll72", false);
        result.add("orderedIds", ids.deepCopy());
        result.add("actualLoadedDefinitionsAndCodecs", rows);
        return result;
    }
    @Mod.EventBusSubscriber(modid = "better_bumblezone_crops", bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static final class Verify {
        private Verify() {}
        @SubscribeEvent
        public static void starting(ServerStartingEvent event) {
            try {
                var data = manifest();
                var server = event.getServer();
                var selected = new ArrayList<>(server.getPackRepository().getSelectedIds());
                require(!selected.isEmpty() && selected.get(selected.size() - 1).equals(PACK_ID), "Canonical pack is not selected at effective TOP: " + selected);
                require(Collections.frequency(selected, PACK_ID) == 1, "Canonical pack duplicated");
                var pack = server.getPackRepository().getPack(PACK_ID);
                require(pack != null && pack.isRequired() && pack.isFixedPosition() && pack.getDefaultPosition() == Pack.Position.TOP,
                    "Canonical pack lost required/fixed-TOP attributes");
                Path snapshot = Path.of(System.getProperty(PROPERTY)).toAbsolutePath().getParent().resolve("pack");
                Path root = Path.of(data.get("root").getAsString());
                var proofRows = new JsonArray();
                for (var element : data.getAsJsonArray("resources")) {
                    var row = element.getAsJsonObject();
                    String path = row.get("resource").getAsString();
                    int split = path.indexOf('/', 5);
                    var id = new ResourceLocation(path.substring(5, split), path.substring(split + 1));
                    var resource = server.getResourceManager().getResource(id).orElseThrow();
                    require(PACK_ID.equals(resource.sourcePackId()), "Canonical effective resource origin drift");
                    String expected = row.get("sha256").getAsString();
                    require(sha(read(snapshot.resolve(path))).equals(expected), "Snapshot byte drift");
                    require(sha(read(root.resolve(row.get("source").getAsString()))).equals(expected), "Root byte drift during startup");
                    try (var input = resource.open()) {
                        byte[] raw = input.readNBytes(MAX_BYTES + 1);
                        require(raw.length <= MAX_BYTES && sha(raw).equals(expected), "Effective native ResourceManager byte drift");
                    }
                    var item = row.deepCopy();
                    item.addProperty("effectiveSourcePackId", resource.sourcePackId());
                    proofRows.add(item);
                }
                var proof = new JsonObject();
                proof.addProperty("schema", "bc.crops_effective_canonical_data.v1");
                proof.addProperty("attempt", data.get("attempt").getAsString());
                proof.addProperty("phase", "ServerStartingEvent_BEFORE_GAMETESTS");
                proof.addProperty("globalIndexProjected", true);
                proof.add("glmProjection", publishGlmScalars());
                proof.add("selectedOrder", new Gson().toJsonTree(selected));
                proof.add("resources", proofRows);
                var context = new JsonArray();
                for (var element : data.getAsJsonArray("context_inputs")) {
                    var row = element.getAsJsonObject();
                    require(sha(read(root.resolve(row.get("source").getAsString()))).equals(row.get("sha256").getAsString()), "Root context byte drift during startup");
                    var item = row.deepCopy();
                    item.addProperty("originKind", "canonical_Root_context_only_NOT_mounted_resource");
                    context.add(item);
                }
                proof.add("contextInputs", context);
                proof.addProperty("scopeSha256", data.get("scope_sha256").getAsString());
                Path output = Path.of(System.getProperty(PROPERTY + ".proof"));
                byte[] bytes = new GsonBuilder().setPrettyPrinting().create().toJson(proof).getBytes(java.nio.charset.StandardCharsets.UTF_8);
                require(bytes.length <= MAX_BYTES, "Oversize canonical proof");
                Files.write(output, bytes, StandardOpenOption.CREATE_NEW);
            } catch (Exception e) { throw new IllegalStateException("Actual canonical ResourceManager proof failed", e); }
        }
    }
}
