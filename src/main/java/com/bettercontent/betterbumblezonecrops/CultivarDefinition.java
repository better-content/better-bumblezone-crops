package com.bettercontent.betterbumblezonecrops;

import java.util.List;

public record CultivarDefinition(
    String id,
    String seedItem,
    List<String> plantBlocks,
    List<String> produce,
    String growthForm,
    String maturityRule,
    String placementAdapter,
    String propagationAnchor,
    List<String> originDimensions
) {}
