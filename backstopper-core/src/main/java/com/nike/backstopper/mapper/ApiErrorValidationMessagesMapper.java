package com.nike.backstopper.mapper;

import com.nike.backstopper.apierror.ApiErrorValue;
import com.nike.backstopper.exception.ApiErrorValidationMessagesMapperException;
import com.nike.backstopper.model.AutoGeneratedApiError;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

/**
 * Maps {@link AutoGeneratedApiError#getMessage()}s to {@code ValidationMessages.properties} files.
 *
 * @author Andrey Tsarenko
 */
public class ApiErrorValidationMessagesMapper {

    private static final String CUSTOM_VALIDATION_MESSAGES_PROPERTIES_NAME = "ValidationMessages.properties";
    private static final String HIBERNATE_VALIDATION_MESSAGES_PROPERTIES = "org/hibernate/validator/ValidationMessages.properties";
    private static final String MESSAGE_EXPRESSION_PREFIX = "{";
    private static final String MESSAGE_EXPRESSION_SUFFIX = "}";

    /**
     * Maps {@link AutoGeneratedApiError#getMessage()}s to properties with next priority:
     * 1. to custom {@code ValidationMessages.properties} files, if the files are present
     * 1. to default {@code org/hibernate/validator/ValidationMessages.properties}, if the files are present
     * otherwise returns the same {@link AutoGeneratedApiError}s back.
     * <p>
     * Internationalization is not currently supported.
     *
     * @param apiErrors the errors based on {@link ApiErrorValue}.
     * @return immutable set of the mapped errors.
     * @throws ApiErrorValidationMessagesMapperException if the mapping failed.
     */
    public Set<AutoGeneratedApiError> mapToValidationMessageProperties(@NotNull Set<AutoGeneratedApiError> apiErrors) {
        try {

            Enumeration<URL> customValidationMessageResources = getClass().getClassLoader()
                    .getResources(CUSTOM_VALIDATION_MESSAGES_PROPERTIES_NAME);
            Enumeration<URL> hibernateValidationMessageResources = getClass().getClassLoader()
                    .getResources(HIBERNATE_VALIDATION_MESSAGES_PROPERTIES);

            return mapByValidationMessageResources(apiErrors,
                    customValidationMessageResources, hibernateValidationMessageResources);
        } catch (Exception e) {
            throw new ApiErrorValidationMessagesMapperException(
                    "Unable to map AutoGeneratedApiErrors to ValidationMessages.properties: " + e.getMessage(), e);
        }
    }

    private Set<AutoGeneratedApiError> mapByValidationMessageResources(
            Set<AutoGeneratedApiError> apiErrors,
            Enumeration<URL> customValidationMessageResources,
            Enumeration<URL> hibernateValidationMessageResources) throws IOException {

        Set<AutoGeneratedApiError> mappedApiErrors = null;
        if (customValidationMessageResources.hasMoreElements()) {
            mappedApiErrors = mapByValidationMessageResource(apiErrors, customValidationMessageResources);
        }

        if (hibernateValidationMessageResources.hasMoreElements()) {
            mappedApiErrors = mapByValidationMessageResource(
                    mappedApiErrors != null ? mappedApiErrors : apiErrors, hibernateValidationMessageResources);
        }
        return mappedApiErrors != null ? mappedApiErrors : apiErrors;
    }

    private Set<AutoGeneratedApiError> mapByValidationMessageResource(
            Set<AutoGeneratedApiError> apiErrors,
            Enumeration<URL> validationMessageResources) throws IOException {

        Set<AutoGeneratedApiError> mappedApiErrors = new HashSet<>();
        while (validationMessageResources.hasMoreElements()) {

            InputStream inputStream = validationMessageResources.nextElement()
                    .openConnection().getInputStream();
            Properties properties = new Properties();
            properties.load(inputStream);
            mappedApiErrors.addAll(mapByValidationMessageProperty(apiErrors, properties));
        }
        return Collections.unmodifiableSet(mappedApiErrors);
    }

    private Set<AutoGeneratedApiError> mapByValidationMessageProperty(Set<AutoGeneratedApiError> apiErrors,
                                                                      Properties validationMessageProperties) {
        Set<AutoGeneratedApiError> mappedApiErrors = new HashSet<>();

        for (AutoGeneratedApiError apiError : apiErrors) {
            String message = apiError.getMessage();

            if (isMessageExpression(message, validationMessageProperties)) {
                String mappedMessage = validationMessageProperties.getProperty(unwrapMessageExpression(message));
                mappedApiErrors.add(new AutoGeneratedApiError(
                        mappedMessage, apiError.getErrorCode(), apiError.getHttpStatusCode()));
            } else {
                mappedApiErrors.add(apiError);
            }
        }
        return Collections.unmodifiableSet(mappedApiErrors);
    }

    private boolean isMessageExpression(String message, Properties validationMessageProperties) {
        return message.length() > (MESSAGE_EXPRESSION_PREFIX.length() + MESSAGE_EXPRESSION_SUFFIX.length())
                && message.startsWith(MESSAGE_EXPRESSION_PREFIX)
                && message.endsWith(MESSAGE_EXPRESSION_SUFFIX)
                && validationMessageProperties.containsKey(unwrapMessageExpression(message));
    }

    private String unwrapMessageExpression(String messageExpression) {
        return messageExpression.substring(MESSAGE_EXPRESSION_PREFIX.length(),
                messageExpression.length() - MESSAGE_EXPRESSION_SUFFIX.length());
    }

}