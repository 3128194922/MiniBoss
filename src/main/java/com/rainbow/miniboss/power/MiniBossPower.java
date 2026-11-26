package com.rainbow.miniboss.power;

import net.minecraft.util.RandomSource;

public enum MiniBossPower {
    DIG("Dig", "挖掘", 1, 2),
    REGENERATE("Regenerate", "自愈", 1, 3),
    REINFORCE("Reinforce", "援军", 1, 3),
    LEADER("Leader", "领袖", 1, 1),
    STEALTH("Stealth", "隐匿", 1, 1),
    EVOLVE("Evolve", "演化", 1, 1),
    PARASITE("Parasite", "寄生", 1, 3),
    ADAPT("Adapt", "适应", 1, 3),
    EXPLODE("Explode", "自爆", 1, 5),
    DISARM("Disarm", "缴械", 1, 5),
    HEAVY_ARMOR("HeavyArmor", "重装", 1, 5),
    DISPEL("Dispel", "驱散", 1, 5),
    SHADOW("Shadow", "暗影", 1, 1),
    ASSASSIN("Assassin", "刺客", 1, 5),
    RIGIDITY("Rigidity", "刚性", 1, 1),
    FURY("Fury", "狂怒", 1, 1),
    DRAIN("Drain", "吸血", 1, 1);

    private final String id;
    private final String label;
    private final int minLevel;
    private final int maxLevel;

    MiniBossPower(String id, String label, int minLevel, int maxLevel) {
        this.id = id;
        this.label = label;
        this.minLevel = minLevel;
        this.maxLevel = maxLevel;
    }

    public String id() {
        return id;
    }

    public String label() {
        return label;
    }

    public int minLevel() {
        return minLevel;
    }

    public int maxLevel() {
        return maxLevel;
    }

    public int rollLevel(RandomSource random) {
        return minLevel + random.nextInt(maxLevel - minLevel + 1);
    }

    public static MiniBossPower byId(String id) {
        if (id == null) return null;
        for (MiniBossPower power : values()) {
            if (power.id.equalsIgnoreCase(id)) return power;
        }
        return null;
    }
}
