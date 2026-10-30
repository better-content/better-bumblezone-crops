package com.bettercontent.betterbumblezonecrops.api;
import org.junit.jupiter.api.Test;
class CultivarAliasRegistryTest {
    @Test void setupOwnershipControls() { CultivarAliasRegistryControls.run(); }
    @Test void callableRuntimeApiVersion() { org.junit.jupiter.api.Assertions.assertEquals(1, CultivarAcquisition.apiVersion()); }
}
