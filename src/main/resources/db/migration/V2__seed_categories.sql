-- This script seeds the initial categories for the platform.
-- We are generating UUIDs directly in the SQL for consistency.

-- First, insert all the main (parent) categories
INSERT INTO categories (id, name, parent_id) VALUES
                                                 (gen_random_uuid(), 'Music', NULL),
                                                 (gen_random_uuid(), 'Arts & Theatre', NULL),
                                                 (gen_random_uuid(), 'Workshops & Education', NULL),
                                                 (gen_random_uuid(), 'Food & Drink', NULL),
                                                 (gen_random_uuid(), 'Sports & Fitness', NULL),
                                                 (gen_random_uuid(), 'Community & Social', NULL);

-- Now, insert the sub-categories, linking them to their parents
-- Sub-categories for 'Music'
INSERT INTO categories (id, name, parent_id) VALUES
                                                 (gen_random_uuid(), 'Concerts', (SELECT id FROM categories WHERE name = 'Music')),
                                                 (gen_random_uuid(), 'DJ Nights', (SELECT id FROM categories WHERE name = 'Music')),
                                                 (gen_random_uuid(), 'Traditional Music', (SELECT id FROM categories WHERE name = 'Music'));

-- Sub-categories for 'Arts & Theatre'
INSERT INTO categories (id, name, parent_id) VALUES
                                                 (gen_random_uuid(), 'Stage Plays', (SELECT id FROM categories WHERE name = 'Arts & Theatre')),
                                                 (gen_random_uuid(), 'Art Exhibitions', (SELECT id FROM categories WHERE name = 'Arts & Theatre')),
                                                 (gen_random_uuid(), 'Film Festivals', (SELECT id FROM categories WHERE name = 'Arts & Theatre'));

-- Sub-categories for 'Workshops & Education'
INSERT INTO categories (id, name, parent_id) VALUES
                                                 (gen_random_uuid(), 'Tech Workshops', (SELECT id FROM categories WHERE name = 'Workshops & Education')),
                                                 (gen_random_uuid(), 'Business Seminars', (SELECT id FROM categories WHERE name = 'Workshops & Education')),
                                                 (gen_random_uuid(), 'Art Classes', (SELECT id FROM categories WHERE name = 'Workshops & Education'));

-- Sub-categories for 'Food & Drink'
INSERT INTO categories (id, name, parent_id) VALUES
                                                 (gen_random_uuid(), 'Food Festivals', (SELECT id FROM categories WHERE name = 'Food & Drink')),
                                                 (gen_random_uuid(), 'Wine Tasting', (SELECT id FROM categories WHERE name = 'Food & Drink'));

-- Sub-categories for 'Sports & Fitness'
INSERT INTO categories (id, name, parent_id) VALUES
                                                 (gen_random_uuid(), 'Cricket Matches', (SELECT id FROM categories WHERE name = 'Sports & Fitness')),
                                                 (gen_random_uuid(), 'Marathons', (SELECT id FROM categories WHERE name = 'Sports & Fitness')),
                                                 (gen_random_uuid(), 'Yoga & Wellness', (SELECT id FROM categories WHERE name = 'Sports & Fitness'));

-- Sub-categories for 'Community & Social'
INSERT INTO categories (id, name, parent_id) VALUES
                                                 (gen_random_uuid(), 'Charity Fundraisers', (SELECT id FROM categories WHERE name = 'Community & Social')),
                                                 (gen_random_uuid(), 'Festivals & Carnivals', (SELECT id FROM categories WHERE name = 'Community & Social')),
                                                 (gen_random_uuid(), 'Meetups', (SELECT id FROM categories WHERE name = 'Community & Social'));
