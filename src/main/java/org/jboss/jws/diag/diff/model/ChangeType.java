package org.jboss.jws.diag.diff.model;

public enum ChangeType {
    /** Present only in right; absent in left. */
    ADDED,
    /** Present only in left; absent in right. */
    REMOVED,
    /** Present in both but values differ. */
    CHANGED
}
