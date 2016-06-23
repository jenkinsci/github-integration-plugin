package com.github.kostyasha.github.integration.generic;

import hudson.model.Action;
import hudson.model.Saveable;

/**
 * @author Kanstantsin Shautsou
 */
public abstract class GitHubRepository<T extends GitHubRepository> implements Action, Saveable {
}
