package com.tagnote.application.resolution.port;

public interface ResolutionConflictTranslator {

    RuntimeException translate(RuntimeException failure);
}
