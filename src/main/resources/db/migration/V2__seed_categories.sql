-- Sample main categories (retain existing)
INSERT INTO categories (id, name, parent_id) VALUES
                                                 (gen_random_uuid(), 'Music', NULL),
                                                 (gen_random_uuid(), 'Arts & Theatre', NULL),
                                                 (gen_random_uuid(), 'Workshops & Education', NULL),
                                                 (gen_random_uuid(), 'Food & Drink', NULL),
                                                 (gen_random_uuid(), 'Sports & Fitness', NULL),
                                                 (gen_random_uuid(), 'Community & Social', NULL),
                                                 (gen_random_uuid(), 'Other', NULL);  -- new catch-all main category

-- Expanded sub-categories

-- Music
INSERT INTO categories (id, name, parent_id) VALUES
                                                 (gen_random_uuid(), 'Concerts', (SELECT id FROM categories WHERE name = 'Music')),
                                                 (gen_random_uuid(), 'DJ Nights', (SELECT id FROM categories WHERE name = 'Music')),
                                                 (gen_random_uuid(), 'Traditional Music', (SELECT id FROM categories WHERE name = 'Music')),
                                                 (gen_random_uuid(), 'Music Competitions', (SELECT id FROM categories WHERE name = 'Music')),   -- e.g., TNL Onstage :contentReference[oaicite:1]{index=1}
                                                 (gen_random_uuid(), 'Music Festivals', (SELECT id FROM categories WHERE name = 'Music'));

-- Arts & Theatre
INSERT INTO categories (id, name, parent_id) VALUES
                                                 (gen_random_uuid(), 'Stage Plays', (SELECT id FROM categories WHERE name = 'Arts & Theatre')),
                                                 (gen_random_uuid(), 'Art Exhibitions', (SELECT id FROM categories WHERE name = 'Arts & Theatre')),
                                                 (gen_random_uuid(), 'Film Festivals', (SELECT id FROM categories WHERE name = 'Arts & Theatre')),
                                                 (gen_random_uuid(), 'Award Ceremonies', (SELECT id FROM categories WHERE name = 'Arts & Theatre')),  -- e.g., Raigam Tele’es, SLIM-Kantar :contentReference[oaicite:2]{index=2}
                                                 (gen_random_uuid(), 'Dance & Performance Arts', (SELECT id FROM categories WHERE name = 'Arts & Theatre'));

-- Workshops & Education
INSERT INTO categories (id, name, parent_id) VALUES
                                                 (gen_random_uuid(), 'Tech Workshops', (SELECT id FROM categories WHERE name = 'Workshops & Education')),
                                                 (gen_random_uuid(), 'Business Seminars', (SELECT id FROM categories WHERE name = 'Workshops & Education')),
                                                 (gen_random_uuid(), 'Art Classes', (SELECT id FROM categories WHERE name = 'Workshops & Education')),
                                                 (gen_random_uuid(), 'Career/Job Fairs', (SELECT id FROM categories WHERE name = 'Workshops & Education')),  -- e.g., EDEX Expo :contentReference[oaicite:3]{index=3}
                                                 (gen_random_uuid(), 'Academic Conferences', (SELECT id FROM categories WHERE name = 'Workshops & Education')),
                                                 (gen_random_uuid(), 'Health & Wellness Workshops', (SELECT id FROM categories WHERE name = 'Workshops & Education'));

-- Food & Drink
INSERT INTO categories (id, name, parent_id) VALUES
                                                 (gen_random_uuid(), 'Food Festivals', (SELECT id FROM categories WHERE name = 'Food & Drink')),
                                                 (gen_random_uuid(), 'Wine Tasting', (SELECT id FROM categories WHERE name = 'Food & Drink')),
                                                 (gen_random_uuid(), 'Street Food Events', (SELECT id FROM categories WHERE name = 'Food & Drink')),
                                                 (gen_random_uuid(), 'Cooking Classes & Demos', (SELECT id FROM categories WHERE name = 'Food & Drink'));

-- Sports & Fitness
INSERT INTO categories (id, name, parent_id) VALUES
                                                 (gen_random_uuid(), 'Cricket Matches', (SELECT id FROM categories WHERE name = 'Sports & Fitness')),
                                                 (gen_random_uuid(), 'Marathons', (SELECT id FROM categories WHERE name = 'Sports & Fitness')),
                                                 (gen_random_uuid(), 'Yoga & Wellness', (SELECT id FROM categories WHERE name = 'Sports & Fitness')),
                                                 (gen_random_uuid(), 'Indoor Sports Tournaments', (SELECT id FROM categories WHERE name = 'Sports & Fitness')),
                                                 (gen_random_uuid(), 'Fitness Bootcamps & Retreats', (SELECT id FROM categories WHERE name = 'Sports & Fitness'));

-- Community & Social
INSERT INTO categories (id, name, parent_id) VALUES
                                                 (gen_random_uuid(), 'Charity Fundraisers', (SELECT id FROM categories WHERE name = 'Community & Social')),
                                                 (gen_random_uuid(), 'Festivals & Carnivals', (SELECT id FROM categories WHERE name = 'Community & Social')),
                                                 (gen_random_uuid(), 'Meetups', (SELECT id FROM categories WHERE name = 'Community & Social')),
                                                 (gen_random_uuid(), 'Religious & Spiritual Events', (SELECT id FROM categories WHERE name = 'Community & Social')),
                                                 (gen_random_uuid(), 'Networking & Social Gatherings', (SELECT id FROM categories WHERE name = 'Community & Social'));

-- Other (catch-all)
INSERT INTO categories (id, name, parent_id) VALUES
                                                 (gen_random_uuid(), 'Virtual / Online Events', (SELECT id FROM categories WHERE name = 'Other')),  -- e.g., webinars :contentReference[oaicite:4]{index=4}
                                                 (gen_random_uuid(), 'Tours & Travel Experiences', (SELECT id FROM categories WHERE name = 'Other')),
                                                 (gen_random_uuid(), 'Expos & Trade Shows', (SELECT id FROM categories WHERE name = 'Other')),  -- includes exhibitions/trade fairs
                                                 (gen_random_uuid(), 'Film Screenings & Movie Events', (SELECT id FROM categories WHERE name = 'Other')),
                                                 (gen_random_uuid(), 'Pop Culture & Fan Events', (SELECT id FROM categories WHERE name = 'Other')),
                                                 (gen_random_uuid(), 'Miscellaneous', (SELECT id FROM categories WHERE name = 'Other'));
