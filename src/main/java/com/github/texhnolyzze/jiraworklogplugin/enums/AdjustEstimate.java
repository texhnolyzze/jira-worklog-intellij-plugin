package com.github.texhnolyzze.jiraworklogplugin.enums;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;

public enum AdjustEstimate {

    NEW("new", "Sets the estimate to a specific value", "New estimate:", "newEstimate") {
        @Override
        public Duration adjust(
            final @NotNull Duration currentEstimate,
            final Duration adjustmentDuration,
            final Duration timeSpent
        ) {
            return adjustmentDuration;
        }
    },

    LEAVE("leave", "Leaves the estimate unchanged", null, null) {
        @Override
        public Duration adjust(
            final @NotNull Duration currentEstimate,
            final Duration adjustmentDuration,
            final Duration timeSpent
        ) {
            return currentEstimate;
        }
    },

    MANUAL("manual", "Reduces the estimate by specified amount", "Reduce by:", "reduceBy") {
        @Override
        public Duration adjust(
            final @NotNull Duration currentEstimate,
            final Duration adjustmentDuration,
            final Duration timeSpent
        ) {
            return subtractAndClamp(currentEstimate, adjustmentDuration);
        }
    },

    AUTO("auto", "Reduces the estimate by time spent", null, null) {
        @Override
        public Duration adjust(
            final @NotNull Duration currentEstimate,
            final @Nullable Duration adjustmentDuration,
            final @Nullable Duration timeSpent
        ) {
            return subtractAndClamp(currentEstimate, timeSpent);
        }
    };

    private final String id;
    private final String description;
    private final String adjustmentDurationLabel;
    private final String adjustmentDurationQueryParameter;

    AdjustEstimate(
        final String id,
        final String description,
        final String adjustmentDurationLabel,
        final String adjustmentDurationQueryParameter
    ) {
        this.id = id;
        this.description = description;
        this.adjustmentDurationLabel = adjustmentDurationLabel;
        this.adjustmentDurationQueryParameter = adjustmentDurationQueryParameter;
    }

    public String getId() {
        return id;
    }

    public String getDescription() {
        return description;
    }

    public String getAdjustmentDurationQueryParameter() {
        return adjustmentDurationQueryParameter;
    }

    @Override
    public String toString() {
        return getId() + " (" + getDescription() + ")";
    }

    public abstract Duration adjust(
        @NotNull final Duration currentEstimate,
        @Nullable final Duration adjustmentDuration,
        @Nullable final Duration timeSpent
    );

    public String getAdjustmentDurationLabel() {
        return adjustmentDurationLabel;
    }

    @Nullable
    private static Duration subtractAndClamp(
        final @NotNull Duration currentEstimate,
        final Duration subtraction
    ) {
        if (subtraction == null) {
            return null;
        }
        final Duration adjusted = currentEstimate.minus(subtraction);
        if (adjusted.isNegative() || adjusted.isZero()) {
            return Duration.ZERO;
        }
        return adjusted;
    }

}
