package com.bettercontent.betterbumblezonecrops;

final class FirethornCuePolicy {
    private FirethornCuePolicy() {}

    static String tooltipKey(String plantId) {
        return "goety:firethorn".equals(plantId)
            ? "item.better_bumblezone_crops.goety_firethorn_seeds.tooltip" : null;
    }
}
