ALTER TABLE contact_inquiries ADD COLUMN preferred_date_tmp DATE;

UPDATE contact_inquiries
SET preferred_date_tmp = CASE
    WHEN preferred_date IS NULL OR TRIM(preferred_date) = '' THEN NULL
    ELSE PARSEDATETIME(preferred_date, 'yyyy-MM-dd')
END;

ALTER TABLE contact_inquiries DROP COLUMN preferred_date;
ALTER TABLE contact_inquiries ALTER COLUMN preferred_date_tmp RENAME TO preferred_date;
