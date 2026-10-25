package com.bettercontent.betterbumblezonecrops.nativefixture;

import com.bettercontent.bettercompatfixes.compat.bumblezone.BumblezoneAtomicConfig;
import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import com.google.gson.JsonObject;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.config.ConfigTracker;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.fml.javafmlmod.FMLModContainer;

/** Exercises the actual optional-provider event and original Forge reload watcher, without altering settings. */
final class BumblezoneConfigWriteProof {
    private BumblezoneConfigWriteProof() {}
    static JsonObject observe() throws Exception {
        var configs = ConfigTracker.INSTANCE.configSets().get(ModConfig.Type.COMMON).stream()
            .filter(config -> config.getModId().equals("the_bumblezone")).toList();
        var expected = Set.of("the_bumblezone/general.toml", "the_bumblezone/worldgen.toml",
            "the_bumblezone/dimension.toml", "the_bumblezone/bee_aggression.toml", "the_bumblezone/mod_compatibility.toml");
        if (!configs.stream().map(ModConfig::getFileName).collect(java.util.stream.Collectors.toSet()).equals(expected))
            throw new IllegalStateException("Native Bumblezone COMMON config population differs");
        for (var config : configs)
            if (!(config.getConfigData() instanceof CommentedFileConfig file) || !BumblezoneAtomicConfig.isInstalled(file))
                throw new IllegalStateException("Actual optional-provider Forge writer was not repaired: " + config.getFileName());
        var general = configs.stream().filter(config -> config.getFileName().equals("the_bumblezone/general.toml")).findFirst().orElseThrow();
        byte[] before = Files.readAllBytes(general.getFullPath());
        var reload = new CountDownLatch(1);
        Consumer<ModConfigEvent.Reloading> listener = event -> { if (event.getConfig() == general) reload.countDown(); };
        var container = (FMLModContainer) ModList.get().getModContainerById("the_bumblezone").orElseThrow();
        var bus = container.getEventBus();
        bus.addListener(EventPriority.LOWEST, false, ModConfigEvent.Reloading.class, listener);
        try {
            general.save();
            if (!reload.await(5, TimeUnit.SECONDS))
                throw new IllegalStateException("Actual Forge config watcher did not reload atomic publication");
            if (!java.util.Arrays.equals(before, Files.readAllBytes(general.getFullPath())))
                throw new IllegalStateException("Native no-op atomic save changed authored Bumblezone settings/comments");
        } finally { bus.unregister(listener); }
        var result = new JsonObject();
        result.addProperty("schema", "bc.crops.bumblezone_config_write.v1");
        result.addProperty("actualCommonWritersObserved", configs.size());
        result.addProperty("originalForgeReloadEventObserved", true);
        result.addProperty("authoredBytesPreserved", true);
        result.addProperty("generalSha256", HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(before)));
        return result;
    }
}
