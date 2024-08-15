-- Create a table to track which sources the user has and has not seen added/removed
CREATE TABLE ui.seen_sources
(
    id integer NOT NULL,
    "user" text NOT NULL,
    external_source_name text NOT NULL,
    external_source_type text NOT NULL,
    derivation_group text NOT NULL,

    constraint seen_sources_pkey
      primary key (id),
    constraint seen_sources_references_user
      foreign key ("user")
      references permissions.users (username) match simple
);

-- Ensure the id is serial
CREATE SEQUENCE ui.seen_sources_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE ui.seen_sources_id_seq OWNED BY ui.seen_sources.id;
ALTER TABLE ONLY ui.seen_sources ALTER COLUMN id SET DEFAULT nextval('ui.seen_sources_id_seq'::regclass);
