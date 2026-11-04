package model;

import java.io.Serializable;

public record Achievement(String id, String title, String description, String icon) implements Serializable {

    public Achievement {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Achievement id must not be empty");
        }
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("Achievement title must not be empty");
        }
    }
}
