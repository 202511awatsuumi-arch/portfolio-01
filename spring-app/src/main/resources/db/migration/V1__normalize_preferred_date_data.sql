UPDATE contact_inquiries
SET preferred_date = CONCAT(
    SUBSTRING(preferred_date, 7, 4),
    '-',
    SUBSTRING(preferred_date, 1, 2),
    '-',
    SUBSTRING(preferred_date, 4, 2)
)
WHERE preferred_date IS NOT NULL
  AND REGEXP_LIKE(preferred_date, '^[0-9]{2}/[0-9]{2}/[0-9]{4}$');
