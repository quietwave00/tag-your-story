package com.tagnote.application.resolution.config;

import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Component
@ConfigurationProperties(prefix = "tag.resolution")
public class TagResolutionProperties {

    private double albumToTrackInheritanceWeight;
    private double minimumScore;

    public void setAlbumToTrackInheritanceWeight(double value) {
        this.albumToTrackInheritanceWeight = requireProbability(value, "Album-to-track inheritance weight");
    }

    public void setMinimumScore(double value) {
        this.minimumScore = requireProbability(value, "Minimum score");
    }

    private double requireProbability(double value, String name) {
        if (!Double.isFinite(value) || value < 0.0 || value > 1.0) {
            throw new IllegalArgumentException(name + " must be between 0.0 and 1.0");
        }
        return value;
    }
}
