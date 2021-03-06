/*
 * Copyright (C) 2012 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package interactivespaces.controller.client.node.internal;

import interactivespaces.SimpleInteractiveSpacesException;
import interactivespaces.activity.configuration.ActivityConfiguration;
import interactivespaces.configuration.Configuration;
import interactivespaces.controller.SpaceController;
import interactivespaces.controller.activity.configuration.LiveActivityConfiguration;
import interactivespaces.controller.activity.wrapper.ActivityWrapper;
import interactivespaces.controller.activity.wrapper.ActivityWrapperFactory;
import interactivespaces.controller.client.node.ActiveControllerActivity;
import interactivespaces.controller.client.node.ActiveControllerActivityFactory;
import interactivespaces.controller.client.node.InternalActivityFilesystem;
import interactivespaces.controller.domain.InstalledLiveActivity;
import interactivespaces.resource.NamedVersionedResourceCollection;
import interactivespaces.resource.Version;
import interactivespaces.resource.VersionRange;

/**
 * An {@link ActiveControllerActivityFactory} which with versioned factories.
 *
 * <p>
 * Factories without a version are given a version of 0.0.0. The highest version
 * available will always be used that meets the activity type's requested
 * version range. No version constraints will give the highest version
 * available.
 *
 * @author Keith M. Hughes
 */
public class SimpleActiveControllerActivityFactory implements ActiveControllerActivityFactory {

  /**
   * The separator between an activity type and a potential version range for
   * the type.
   */
  public static final char VERSION_RANGE_SEPARATOR = ';';

  /**
   * Collection of wrapper factories.
   */
  private final NamedVersionedResourceCollection<ActivityWrapperFactory> activityWrapperFactories =
      NamedVersionedResourceCollection.newNamedVersionedResourceCollection();

  @Override
  public ActiveControllerActivity createActiveLiveActivity(String activityType, InstalledLiveActivity liveActivity,
      InternalActivityFilesystem activityFilesystem, LiveActivityConfiguration configuration, SpaceController controller) {

    String bareActivityType = activityType;
    VersionRange versionRange = null;
    int versionRangePos = bareActivityType.indexOf(VERSION_RANGE_SEPARATOR);
    if (versionRangePos != -1) {
      versionRange = VersionRange.parseVersionRange(bareActivityType.substring(versionRangePos + 1));
      bareActivityType = bareActivityType.substring(0, versionRangePos);
    }
    bareActivityType = bareActivityType.toLowerCase();

    ActivityWrapperFactory wrapperFactory = null;
    if (versionRange != null) {
      wrapperFactory = activityWrapperFactories.getResource(bareActivityType, versionRange);
    } else {
      wrapperFactory = activityWrapperFactories.getHighestResource(bareActivityType);
    }

    if (wrapperFactory != null) {
      ActivityWrapper wrapper =
          wrapperFactory.newActivityWrapper(liveActivity, activityFilesystem, configuration, controller);

      ActiveControllerActivity activeLiveActivity =
          new ActiveControllerActivity(liveActivity, wrapper, activityFilesystem, configuration, controller);

      return activeLiveActivity;
    } else {
      String message =
          String
              .format("Unsupported activity type %s for activity %s", activityType.toString(), liveActivity.getUuid());
      controller.getSpaceEnvironment().getLog().warn(message);

      throw new SimpleInteractiveSpacesException(message);
    }
  }

  @Override
  public ActiveControllerActivity newActiveActivity(InstalledLiveActivity liveActivity,
      InternalActivityFilesystem activityFilesystem, LiveActivityConfiguration configuration, SpaceController controller) {
    String type = getConfiguredType(configuration);

    return createActiveLiveActivity(type, liveActivity, activityFilesystem, configuration, controller);
  }

  @Override
  public String getConfiguredType(Configuration configuration) {
    return configuration.getRequiredPropertyString(ActivityConfiguration.CONFIGURATION_ACTIVITY_TYPE);
  }

  @Override
  public void registerActivityWrapperFactory(ActivityWrapperFactory factory) {
    Version version = getFactoryVersion(factory);

    if (activityWrapperFactories.addResource(factory.getActivityType().toLowerCase(), version, factory) != null) {
      throw new SimpleInteractiveSpacesException(String.format("The %s %s is already registered",
          factory.getActivityType(), ActivityWrapperFactory.class.getName()));
    }
  }

  @Override
  public void unregisterActivityWrapperFactory(ActivityWrapperFactory factory) {
    Version version = getFactoryVersion(factory);

    String activityType = factory.getActivityType().toLowerCase();
    if (activityWrapperFactories.getResource(activityType, new VersionRange(version)) == factory) {
      activityWrapperFactories.removeResource(activityType, version);
    }
  }

  /**
   * Get the version of the factory.
   *
   * @param factory
   *          the factory
   *
   * @return the version to use
   */
  private Version getFactoryVersion(ActivityWrapperFactory factory) {
    // No version gets a version of 0.0.0.
    Version version = factory.getVersion();
    if (version == null) {
      version = new Version(0, 0, 0);
    }
    return version;
  }
}
