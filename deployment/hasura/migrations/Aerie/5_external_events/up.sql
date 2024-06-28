-- Create a table to represent external event sources.
CREATE TABLE merlin.external_source (
    id integer NOT NULL,
    key text NOT NULL,
    file_id integer NOT NULL,
    source_type_id integer NOT NULL,
    valid_at timestamp with time zone NOT NULL,
    start_time timestamp with time zone NOT NULL,
    end_time timestamp with time zone NOT NULL,
    metadata jsonb
);

COMMENT ON TABLE merlin.external_source IS 'A table for externally imported event sources.';

-- Ensure the id is serial.
CREATE SEQUENCE merlin.external_source_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE merlin.external_source_id_seq OWNED BY merlin.external_source.id;
ALTER TABLE ONLY merlin.external_source ALTER COLUMN id SET DEFAULT nextval('merlin.external_source_id_seq'::regclass);

-- Set primary key
ALTER TABLE ONLY merlin.external_source
    ADD CONSTRAINT external_source_pkey PRIMARY KEY (id);

-- Add foreign key definition for file_id field, linking to uploaded_file table
ALTER TABLE ONLY merlin.external_source
    ADD CONSTRAINT "file_id -> uploaded_file" FOREIGN KEY (file_id) REFERENCES merlin.uploaded_file(id);




-- Create table for external events
CREATE TABLE merlin.external_event (
    id integer NOT NULL,
    key text NOT NULL,
    event_type_id integer NOT NULL,
    source_id integer NOT NULL,
    start_time timestamp with time zone NOT NULL,
    duration time without time zone NOT NULL,
    properties jsonb
);

COMMENT ON TABLE merlin.external_event IS 'A table for externally imported events.';


-- Ensure the id is serial.
CREATE SEQUENCE merlin.external_event_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE merlin.external_event_id_seq OWNED BY merlin.external_event.id;
ALTER TABLE ONLY merlin.external_event ALTER COLUMN id SET DEFAULT nextval('merlin.external_event_id_seq'::regclass);


-- Set primary key
ALTER TABLE ONLY merlin.external_event
    ADD CONSTRAINT external_event_pkey PRIMARY KEY (id);


-- Add uniqueness constraint for key/source_id/event_type_id tuple
ALTER TABLE ONLY merlin.external_event
    ADD CONSTRAINT logical_identifiers UNIQUE (key, source_id, event_type_id);

COMMENT ON CONSTRAINT logical_identifiers ON merlin.external_event IS 'The tuple (key, event_type_id, and source_id) must be unique!';


-- Add foreign key linking the source_id to the id of an external_source entry
ALTER TABLE ONLY merlin.external_event
    ADD CONSTRAINT "source_id -> external_source.id" FOREIGN KEY (source_id) REFERENCES merlin.external_source(id);




-- Create table for plan/external event links
CREATE TABLE merlin.plan_external_source (
    id integer NOT NULL,
    plan_id integer NOT NULL,
    external_source_id integer NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    owner text
);

COMMENT ON TABLE merlin.plan_external_source IS 'A table for linking externally imported event sources & plans.';

-- Ensure the id is serial
CREATE SEQUENCE merlin.plan_external_source_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE merlin.plan_external_source_id_seq OWNED BY merlin.plan_external_source.id;
ALTER TABLE ONLY merlin.plan_external_source ALTER COLUMN id SET DEFAULT nextval('merlin.plan_external_source_id_seq'::regclass);


-- Set primary key
ALTER TABLE ONLY merlin.plan_external_source
    ADD CONSTRAINT plan_external_source_pkey PRIMARY KEY (id);


-- Add uniqueness constraint for plan_id/external_source_id pair
ALTER TABLE ONLY merlin.plan_external_source
    ADD CONSTRAINT unique_plan_external_source UNIQUE (plan_id, external_source_id);

COMMENT ON CONSTRAINT unique_plan_external_source ON merlin.plan_external_source IS 'The tuple (plan_id, external_source_id) must be unique!';

-- Add foreign keys
ALTER TABLE ONLY merlin.plan_external_source
    ADD CONSTRAINT fk_external_source_id FOREIGN KEY (external_source_id) REFERENCES merlin.external_source(id);

ALTER TABLE ONLY merlin.plan_external_source
    ADD CONSTRAINT fk_plan_id FOREIGN KEY (plan_id) REFERENCES merlin.plan(id);




-- Create table for external source types
CREATE TABLE merlin.external_source_type (
    id integer NOT NULL,
    name text NOT NULL
);

COMMENT ON TABLE merlin.external_source_type IS 'A table for externally imported event source types.';

-- Ensure the id is serial.
CREATE SEQUENCE merlin.external_source_type_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE merlin.external_source_type_id_seq OWNED BY merlin.external_source_type.id;
ALTER TABLE ONLY merlin.external_source_type ALTER COLUMN id SET DEFAULT nextval('merlin.external_source_type_id_seq'::regclass);

-- Set primary key
ALTER TABLE ONLY merlin.external_source_type
    ADD CONSTRAINT external_source_type_pkey PRIMARY KEY (id);

-- TODO: Come back to this when we want to tackle versioning
-- Add uniqueness constraint for name & version
--ALTER TABLE ONLY merlin.external_source_type
--    ADD CONSTRAINT logical_identifiers UNIQUE (name, version);
-- COMMENT ON CONSTRAINT logical_identifiers ON merlin.external_source_type IS 'The tuple (name, version) must be unique!';

-- Update merlin.external_source.source_type_id to link it to merlin.external_source_type.id
ALTER TABLE ONLY merlin.external_source
    ADD CONSTRAINT "source_type_id -> external_source_type" FOREIGN KEY (source_type_id) REFERENCES merlin.external_source_type(id);




-- Create table for external event types
CREATE TABLE merlin.external_event_type (
    id integer NOT NULL,
    name text NOT NULL
);

COMMENT ON TABLE merlin.external_event_type IS 'A table for externally imported event types.';

-- Ensure the id is serial.
CREATE SEQUENCE merlin.external_event_type_id_seq
    AS integer
    START WITH 1
    INCREMENT by 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE merlin.external_event_type_id_seq OWNED BY merlin.external_event_type.id;
ALTER TABLE ONLY merlin.external_event_type ALTER COLUMN id SET DEFAULT nextval('merlin.external_event_type_id_seq'::regclass);

-- Set primary key
ALTER TABLE ONLY merlin.external_event_type
    ADD CONSTRAINT external_event_type_pkey PRIMARY KEY (id);

-- TODO: consider implementing versioning in a future batch of work

ALTER TABLE ONLY merlin.external_event
    ADD CONSTRAINT "event_type_id -> external_event_type" FOREIGN KEY (event_type_id) REFERENCES merlin.external_event_type(id);




-- Create table mapping external sources to their contained event types
CREATE TABLE merlin.external_source_event_types (
    id integer NOT NULL,
    external_source_id integer NOT NULL,
    external_event_type_id integer NOT NULL
);

COMMENT ON TABLE merlin.external_source_event_types IS 'A table detailing the event types that a given external_source has.';

-- Ensure the id is serial.
CREATE SEQUENCE merlin.external_source_event_types_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE merlin.external_source_event_types_id_seq OWNED BY merlin.external_source_event_types.id;
ALTER TABLE ONLY merlin.external_source_event_types ALTER COLUMN id SET DEFAULT nextval('merlin.external_source_event_types_id_seq'::regclass);

-- Set primary key
ALTER TABLE ONLY merlin.external_source_event_types
    ADD CONSTRAINT external_source_event_type_pkey PRIMARY KEY (id);

-- Add key for external_event_type_id
ALTER TABLE ONLY merlin.external_source_event_types
    ADD CONSTRAINT external_event_type_id FOREIGN KEY (external_event_type_id) REFERENCES merlin.external_event_type(id);

-- Add key for external_source_id
ALTER TABLE ONLY merlin.external_source_event_types
    ADD CONSTRAINT external_source_id FOREIGN KEY (external_source_id) REFERENCES merlin.external_source(id);

call migrations.mark_migration_applied('5');
