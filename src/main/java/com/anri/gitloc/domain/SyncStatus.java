package com.anri.gitloc.domain;

/**
 * Статус последнего синхронизированного сервиса.
 */
public enum SyncStatus {
    NEVER_SYNCED,
    RUNNING,
    OK,
    FAILED
}