-- Script to generate 100,000 links for load testing
-- 1000 users (chats) with 100 links each

-- Generate 1000 test chats
INSERT INTO chats (chat_id, created_at)
SELECT 
    1000000 + generate_series(1, 1000) AS chat_id,
    NOW() AS created_at
ON CONFLICT (chat_id) DO NOTHING;

-- Generate 100,000 test links (100 links per user)
-- Using GitHub and StackOverflow URLs
WITH test_links AS (
    SELECT 
        1000000 + (generate_series(1, 100000) / 100) AS chat_id,
        CASE 
            WHEN generate_series(1, 100000) % 2 = 0 
            THEN 'https://github.com/user' || (generate_series(1, 100000) % 10000) || '/repo' || (generate_series(1, 100000) % 10)
            ELSE 'https://stackoverflow.com/questions/' || (1000000 + generate_series(1, 100000))
        END AS url,
        NOW() AS created_at,
        NOW() AS last_checked_at
)
INSERT INTO links (url, created_at, last_checked_at)
SELECT DISTINCT url, created_at, last_checked_at
FROM test_links
ON CONFLICT (url) DO NOTHING;

-- Link chats with links
INSERT INTO link_chat (link_id, chat_id)
SELECT 
    l.link_id,
    1000000 + ((ROW_NUMBER() OVER (ORDER BY l.link_id) - 1) / 100) AS chat_id
FROM links l
WHERE l.url LIKE 'https://github.com/user%' OR l.url LIKE 'https://stackoverflow.com/questions/1%'
ON CONFLICT (link_id, chat_id) DO NOTHING
LIMIT 100000;

-- Verify data
SELECT 
    'Total chats' AS metric,
    COUNT(*) AS count
FROM chats
WHERE chat_id >= 1000000

UNION ALL

SELECT 
    'Total links' AS metric,
    COUNT(*) AS count
FROM links
WHERE url LIKE 'https://github.com/user%' OR url LIKE 'https://stackoverflow.com/questions/1%'

UNION ALL

SELECT 
    'Total link_chat relations' AS metric,
    COUNT(*) AS count
FROM link_chat
WHERE chat_id >= 1000000;
