-- AI-generated bio, written asynchronously after person creation.
-- NULL until generation completes (or forever, if generation is disabled/fails).
ALTER TABLE person ADD COLUMN bio VARCHAR(1024);
