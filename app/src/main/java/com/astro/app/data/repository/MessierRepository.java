package com.astro.app.data.repository;

import com.astro.app.data.model.MessierObjectData;

import java.util.List;

/**
 * Repository interface for accessing Messier / Deep Sky Object data.
 */
public interface MessierRepository {
    List<MessierObjectData> getAllObjects();
    MessierObjectData getById(String id);
    List<MessierObjectData> search(String query);
}
