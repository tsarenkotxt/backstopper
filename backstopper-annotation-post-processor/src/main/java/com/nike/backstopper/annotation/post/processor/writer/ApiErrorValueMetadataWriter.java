package com.nike.backstopper.annotation.post.processor.writer;

import com.nike.backstopper.annotation.post.processor.exception.ApiErrorValueMetadataWriterException;
import com.nike.backstopper.model.ApiErrorValueMetadata;
import org.jetbrains.annotations.NotNull;

import javax.annotation.processing.Filer;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.Set;

import static com.nike.backstopper.service.AutoGeneratedApiErrorService.API_ERROR_VALUE_METADATA_PATH;
import static javax.tools.StandardLocation.CLASS_OUTPUT;

/**
 * Writes {@link ApiErrorValueMetadata}s to {@code META-INF/api-error-value-metadata} file.
 *
 * @author Andrey Tsarenko
 */
public class ApiErrorValueMetadataWriter {

    private final Filer filer;

    public ApiErrorValueMetadataWriter(Filer filer) {
        this.filer = filer;
    }

    /**
     * Writes serialized {@link ApiErrorValueMetadata}s to {@code META-INF/api-error-value-metadata} file.
     *
     * @param apiErrorValuesMetadata the metadata to write.
     * @throws ApiErrorValueMetadataWriterException if the writing failed.
     */
    public void write(@NotNull Set<ApiErrorValueMetadata> apiErrorValuesMetadata) {
        if (!apiErrorValuesMetadata.isEmpty()) {

            try (OutputStream outputStream = filer.createResource(CLASS_OUTPUT, "", API_ERROR_VALUE_METADATA_PATH)
                    .openOutputStream()) {

                ObjectOutputStream objectOutputStream = new ObjectOutputStream(outputStream);
                objectOutputStream.writeObject(apiErrorValuesMetadata);
                objectOutputStream.flush();
            } catch (Exception e) {
                throw new ApiErrorValueMetadataWriterException("Unable to write a metadata: " + e.getMessage(), e);
            }
        }
    }

}
