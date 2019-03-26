package au.gov.ga.geodesy.domain.model.command;

import au.gov.ga.geodesy.domain.model.sitelog.EffectiveDates;

public class RemoveCorsSiteFromNetwork {

    private Integer networkId;
    private EffectiveDates period = new EffectiveDates();

    public RemoveCorsSiteFromNetwork(Integer networkId, EffectiveDates period) {
        this.networkId = networkId;
        this.period = period;
    }

    public Integer getNetworkId() {
        return networkId;
    }

    public EffectiveDates getPeriod() {
        return period;
    }
}
