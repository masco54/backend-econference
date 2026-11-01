-- Simplify EventStatus: PENDING and DRAFT both become APPROVED.
-- These statuses represented events awaiting or ready for organizer configuration,
-- which is now unified under APPROVED.
UPDATE events SET status = 'APPROVED' WHERE status IN ('PENDING', 'DRAFT');

-- Update the column default
ALTER TABLE events ALTER COLUMN status SET DEFAULT 'APPROVED';
