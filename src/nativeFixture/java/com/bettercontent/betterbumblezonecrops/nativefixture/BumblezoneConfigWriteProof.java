package com.bettercontent.betterbumblezonecrops.nativefixture;

import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import com.google.gson.JsonObject;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
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

/** Independent functional observation; no provider compile API, copied repair or private access. */
final class BumblezoneConfigWriteProof {
    private BumblezoneConfigWriteProof() {}
    static JsonObject observe() throws Exception {
        var configs = ConfigTracker.INSTANCE.configSets().get(ModConfig.Type.COMMON).stream()
            .filter(config -> config.getModId().equals("the_bumblezone")).toList();
        var expected = Set.of("the_bumblezone/general.toml", "the_bumblezone/worldgen.toml",
            "the_bumblezone/dimension.toml", "the_bumblezone/bee_aggression.toml", "the_bumblezone/mod_compatibility.toml");
        if (configs.size() != expected.size() || !configs.stream().map(ModConfig::getFileName).collect(java.util.stream.Collectors.toSet()).equals(expected))
            throw new IllegalStateException("Native Bumblezone COMMON config population differs");
        var general = configs.stream().filter(config -> config.getFileName().equals("the_bumblezone/general.toml")).findFirst().orElseThrow();
        byte[] generalBefore = Files.readAllBytes(general.getFullPath());
        var reload = new CountDownLatch(1);
        Consumer<ModConfigEvent.Reloading> listener = event -> { if (event.getConfig() == general) reload.countDown(); };
        var container = (FMLModContainer) ModList.get().getModContainerById("the_bumblezone").orElseThrow();
        var bus = container.getEventBus();
        bus.addListener(EventPriority.LOWEST, false, ModConfigEvent.Reloading.class, listener);
        try {
            for (var config : configs) {
                if (!(config.getConfigData() instanceof CommentedFileConfig))
                    throw new IllegalStateException("Actual Forge config is not file-backed: " + config.getFileName());
                Path path = config.getFullPath();
                byte[] before = Files.readAllBytes(path);
                Object previousInode = fileIdentity(path);
                config.save(); // The ACTUAL original Forge API, not a call to the provider's writer.
                if (previousInode.equals(fileIdentity(path)))
                    throw new IllegalStateException("Actual Forge save did not replace the complete file inode: " + config.getFileName());
                if (!java.util.Arrays.equals(before, Files.readAllBytes(path)))
                    throw new IllegalStateException("Native no-op save changed authored settings/comments: " + config.getFileName());
            }
            if (!reload.await(5, TimeUnit.SECONDS))
                throw new IllegalStateException("Original Forge config watcher did not reload atomic publication");
        } finally { bus.unregister(listener); }
        var result = new JsonObject();
        result.addProperty("schema", "bc.crops.bumblezone_config_write.v1");
        result.addProperty("actualCommonWritersObserved", configs.size());
        result.addProperty("actualFileInodesReplaced", configs.size());
        result.addProperty("originalForgeReloadEventObserved", true);
        result.addProperty("authoredBytesPreserved", true);
        result.addProperty("generalSha256", HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(generalBefore)));
        return result;
    }
    private static Object fileIdentity(Path path) throws Exception {
        Object key = Files.readAttributes(path, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS).fileKey();
        if (key == null) throw new IllegalStateException("Native atomic publication proof requires filesystem file identity");
        return key;
    }
}
