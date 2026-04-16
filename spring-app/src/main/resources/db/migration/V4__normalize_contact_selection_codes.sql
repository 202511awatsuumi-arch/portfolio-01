UPDATE contact_inquiries
SET contact_method = CASE
    WHEN contact_method IN ('メール', 'Email') THEN 'EMAIL'
    WHEN contact_method IN ('電話', 'Phone') THEN 'PHONE'
    ELSE contact_method
END
WHERE contact_method IN ('メール', 'Email', '電話', 'Phone');

UPDATE contact_inquiries
SET plan = CASE
    WHEN plan IN ('スタンダードコース', 'Standard Course') THEN 'STANDARD'
    WHEN plan IN ('プレミアムコース', 'Premium Course') THEN 'PREMIUM'
    WHEN plan IN ('相談してきめる', 'I’d like to decide after consultation', 'I''d like to decide after consultation') THEN 'CONSULT'
    ELSE plan
END
WHERE plan IN (
    'スタンダードコース',
    'Standard Course',
    'プレミアムコース',
    'Premium Course',
    '相談してきめる',
    'I’d like to decide after consultation',
    'I''d like to decide after consultation'
);

UPDATE contact_inquiries
SET request_types = REPLACE(
    REPLACE(
        REPLACE(
            REPLACE(
                REPLACE(
                    REPLACE(request_types,
                        '予約希望', 'BOOKING_REQUEST'),
                    'Booking Request', 'BOOKING_REQUEST'),
                '空き状況確認', 'AVAILABILITY_CHECK'),
            'Availability Check', 'AVAILABILITY_CHECK'),
        'プラン相談', 'PLAN_CONSULTATION'),
    'Plan Consultation', 'PLAN_CONSULTATION')
WHERE request_types IS NOT NULL;
