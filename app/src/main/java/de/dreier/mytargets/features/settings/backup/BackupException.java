/*
 * Copyright (C) 2016 Florian Dreier
 *
 * This file is part of MyTargets.
 *
 * MyTargets is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2
 * as published by the Free Software Foundation.
 *
 * MyTargets is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 */

package de.dreier.mytargets.features.settings.backup;

public class BackupException extends Exception {
    private final String message;

    public BackupException(String message) {
        this.message = message;
    }

    public BackupException(String message, Exception e) {
        super(e);
        this.message = message;
    }

    @Override
    public String getMessage() {
        return message;
    }
}
