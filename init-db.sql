-- Create database for bot if it doesn't exist
SELECT 'CREATE DATABASE link_tracker_bot'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'link_tracker_bot')\gexec
