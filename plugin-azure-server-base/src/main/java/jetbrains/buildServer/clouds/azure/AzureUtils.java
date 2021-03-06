/*
 * Copyright 2000-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package jetbrains.buildServer.clouds.azure;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import jetbrains.buildServer.clouds.CloudClientParameters;
import jetbrains.buildServer.clouds.CloudImageParameters;
import jetbrains.buildServer.clouds.CloudInstanceUserData;
import jetbrains.buildServer.clouds.base.beans.CloudImageDetails;
import jetbrains.buildServer.clouds.base.beans.CloudImagePasswordDetails;
import jetbrains.buildServer.util.StringUtil;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;

/**
 * Provides utils for azure services.
 */
public final class AzureUtils {
    private static final Type stringStringMapType = new TypeToken<Map<String, String>>() {
    }.getType();

    public static <T extends CloudImageDetails> Collection<T> parseImageData(Class<T> clazz, final CloudClientParameters params) {
        Gson gson = new Gson();
        final String imageData = StringUtil.notEmpty(
                params.getParameter(CloudImageParameters.SOURCE_IMAGES_JSON),
                StringUtil.emptyIfNull(params.getParameter("images_data")));
        if (StringUtil.isEmpty(imageData)) {
            return Collections.emptyList();
        }

        final ListParameterizedType listType = new ListParameterizedType(clazz);
        final List<T> images = gson.fromJson(imageData, listType);
        if (CloudImagePasswordDetails.class.isAssignableFrom(clazz)) {
            final String passwordData = params.getParameter("secure:passwords_data");
            final Map<String, String> data = gson.fromJson(passwordData, stringStringMapType);
            if (data != null) {
                for (T image : images) {
                    final CloudImagePasswordDetails userImage = (CloudImagePasswordDetails) image;
                    if (data.get(image.getSourceId()) != null) {
                        userImage.setPassword(data.get(image.getSourceId()));
                    }
                }
            }
        }

        return new ArrayList<>(images);
    }

    private static class ListParameterizedType implements ParameterizedType {

        private Type type;

        private ListParameterizedType(Type type) {
            this.type = type;
        }

        @Override
        public Type[] getActualTypeArguments() {
            return new Type[]{type};
        }

        @Override
        public Type getRawType() {
            return ArrayList.class;
        }

        @Override
        public Type getOwnerType() {
            return null;
        }

        // implement equals method too! (as per javadoc)
    }

    /**
     * Updates tag data.
     *
     * @param tag    original tag.
     * @param vmName virtual machine name.
     * @return updated tag.
     */
    public static CloudInstanceUserData setVmNameForTag(@NotNull final CloudInstanceUserData tag, @NotNull final String vmName) {
        return new CloudInstanceUserData(vmName,
                tag.getAuthToken(),
                tag.getServerAddress(),
                tag.getIdleTimeout(),
                tag.getProfileId(),
                tag.getProfileDescription(),
                tag.getCustomAgentConfigurationParameters());
    }
}
