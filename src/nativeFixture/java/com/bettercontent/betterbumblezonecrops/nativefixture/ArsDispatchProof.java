package com.bettercontent.betterbumblezonecrops.nativefixture;

import com.google.gson.*;
import com.hollingsworth.arsnouveau.api.loot.DungeonLootEnhancerModifier;
import com.hollingsworth.arsnouveau.setup.registry.LootRegistry;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraftforge.common.loot.IGlobalLootModifier;
import net.minecraftforge.registries.ForgeRegistries;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

/** Ars-only synchronous original-call observers. References are transient parameters/locals only. */
public final class ArsDispatchProof {
    private static final ArsDispatchLedger LEDGER = new ArsDispatchLedger();
    private ArsDispatchProof() {}
    public static boolean active() { return LEDGER.activeArsParse(); }
    public static boolean fullSuccess(DataResult<?> result) {
        return result != null && result.error().isEmpty() && result.result().isPresent();
    }
    public static boolean sameRoot(Object receiver, Object originalRoot) {
        return receiver != null && receiver == originalRoot;
    }
    public static boolean selectedIdentity(Object receiver, Object registryDecoder, Object selected,
            Object arsRegistered, Object registryRegistered, boolean exactKey) {
        return receiver != null && receiver == registryDecoder && selected != null
            && selected == arsRegistered && selected == registryRegistered && exactKey;
    }
    public static boolean publicBodyFields(JsonObject data, double common, double uncommon, double rare,
            int commonRolls, int uncommonRolls, int rareRolls) {
        return Double.doubleToLongBits(common) == Double.doubleToLongBits(data.get("common_chance").getAsDouble())
            && Double.doubleToLongBits(uncommon) == Double.doubleToLongBits(data.get("uncommon_chance").getAsDouble())
            && Double.doubleToLongBits(rare) == Double.doubleToLongBits(data.get("rare_chance").getAsDouble())
            && commonRolls == data.get("common_rolls").getAsInt()
            && uncommonRolls == data.get("uncommon_rolls").getAsInt()
            && rareRolls == data.get("rare_rolls").getAsInt();
    }
    private static String digest(Object json) throws Exception {
        if (!(json instanceof JsonElement)) throw new IllegalArgumentException("Expected original JSON input");
        byte[] bytes = json.toString().getBytes(StandardCharsets.UTF_8);
        if (bytes.length > 256 * 1024) throw new IllegalArgumentException("Ars input overflow");
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
    }
    public static void begin() {
        try {
            var ids = CanonicalDataFixture.codecProofIds();
            LEDGER.begin(ids);
            OriginalCodecDispatchProof.begin(ids);
        }
        catch (Exception error) { LEDGER.fail("APPLY_INPUT_BINDING"); LEDGER.end(); }
    }
    public static void before(ResourceManager resources, ResourceLocation row, JsonElement input) {
        OriginalCodecDispatchProof.before(row, input);
        try {
            String id = row.toString();
            if (ArsDispatchLedger.ID.equals(id)) CanonicalDataFixture.verifyOriginalArsInput(resources, input);
            LEDGER.before(id, ArsDispatchLedger.ID.equals(id) ? digest(input) : null);
        } catch (Exception error) { LEDGER.fail("ROW_INPUT_BINDING"); }
    }
    public static void after(ResourceLocation row) {
        OriginalCodecDispatchProof.after(row);
        try { LEDGER.after(row.toString()); }
        catch (Exception error) { LEDGER.fail("AFTER_ROW_BINDING"); }
    }
    public static void registered(Object receiver, DynamicOps<?> ops, Object input, DataResult<?> result) {
        OriginalCodecDispatchProof.registered(receiver, ops, input, result);
        if (!active()) return; // MUST precede all registry/Ars/FML/input resolution
        try {
            var registry = ForgeRegistries.GLOBAL_LOOT_MODIFIER_SERIALIZERS.get();
            var decoder = registry.getCodec();
            if (receiver != decoder) return;
            var key = new ResourceLocation(ArsDispatchLedger.ID);
            Object payload = result == null ? null : result.result().orElse(null);
            Object selected = payload instanceof Pair<?, ?> pair ? pair.getFirst() : null;
            var expected = registry.getValue(key);
            boolean identity = selectedIdentity(receiver, decoder, selected,
                LootRegistry.STRUCTURE_MODDED_LOOT_IMPORTER.get(), expected, key.equals(registry.getKey(expected)));
            boolean exactInput = input instanceof JsonPrimitive value && value.isString()
                && ArsDispatchLedger.ID.equals(value.getAsString());
            LEDGER.registry(true, ops == JsonOps.INSTANCE && exactInput,
                fullSuccess(result), identity,
                identity ? key.toString() : null);
        } catch (Exception error) { LEDGER.fail("REGISTERED_RESULT_OBSERVATION"); }
    }
    private static boolean bodyFields(DungeonLootEnhancerModifier body, Object input) {
        if (!(input instanceof JsonObject data)) return false;
        return publicBodyFields(data, body.commonChance, body.uncommonChance, body.rareChance,
            body.commonRolls, body.uncommonRolls, body.rareRolls);
    }
    public static void bodyParse(Object receiver, DynamicOps<?> ops, Object input, DataResult<?> result) {
        OriginalCodecDispatchProof.body(receiver, ops, input, result);
        if (!active()) return; // no DIRECT_CODEC or Ars initialization during unrelated bootstrap
        try {
            if (!sameRoot(receiver, IGlobalLootModifier.DIRECT_CODEC)) return;
            // Codec.parse returns the decoded body, unlike Codec.decode's Pair<body, remainder>.
            Object value = result == null ? null : result.result().orElse(null);
            boolean exactBody = value != null && value.getClass().getName().equals(ArsDispatchLedger.BODY)
                && value instanceof DungeonLootEnhancerModifier ars && bodyFields(ars, input);
            LEDGER.body(true, ops == JsonOps.INSTANCE,
                fullSuccess(result) && exactBody,
                digest(input), value == null ? null : value.getClass().getName());
        } catch (Exception error) { LEDGER.fail("ROOT_RESULT_OBSERVATION"); }
    }
    public static void requireLoaded(int ordinal, IGlobalLootModifier loaded, JsonElement expectedBody) {
        LEDGER.requireComplete(ordinal, loaded.getClass().getName());
        if (!(loaded instanceof DungeonLootEnhancerModifier ars) || !bodyFields(ars, expectedBody))
            throw new IllegalStateException("Actual loaded Ars body differs from original successful decode");
    }
    public static void end() { LEDGER.end(); OriginalCodecDispatchProof.end(); }
}
