-- Create a table to track which sources the user has and has not seen added/removed
create table ui.seen_sources
(
    username text not null,
    external_source_name text not null,
    external_source_type text not null,
    derivation_group text not null,

    constraint seen_sources_pkey
      primary key (username, external_source_name, derivation_group), -- is this a good pkey?
    constraint seen_sources_references_user
      foreign key (username)
      references permissions.users (username) match simple
);

comment on table ui.seen_sources is e''
  'A table for tracking the external sources acknowledge/unacknowledged by each user.';
