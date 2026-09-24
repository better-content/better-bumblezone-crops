package com.bettercontent.bumblezonecultivars;

final class FirethornCuePolicy {
    private FirethornCuePolicy() {}

    static String tooltipKey(String plantId) {
        return "goety:firethorn".equals(plantId)
            ? "item.bumblezone_cultivars.goety_firethorn_seeds.tooltip" : null;
    }
}
