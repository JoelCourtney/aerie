create table expansion_set_to_rule (
  set_id integer not null,
  rule_id integer not null,
  activity_type text not null,

  constraint expansion_set_to_rule_primary_key
  primary key (set_id, rule_id),

  foreign key (set_id)
  references expansion_set (id),

  foreign key (rule_id, activity_type)
  references expansion_rule (id, activity_type),

  unique (set_id, activity_type)
);
comment on table expansion_set_to_rule is e''
  'The join table between expansion_set and expansion_rule';
comment on column expansion_set_to_rule.set_id is e''
  'The id for an expansion_set.';
comment on column expansion_set_to_rule.rule_id is e''
  'the id for an expansion_rule.';

create view expansion_set_rule_view as
  select set_id, expansion_rule.*
  from expansion_set_to_rule left join expansion_rule
  on expansion_set_to_rule.rule_id = expansion_rule.id;

create view rule_expansion_set_view as
  select rule_id, expansion_set.*
  from expansion_set_to_rule left join expansion_set
  on expansion_set_to_rule.set_id = expansion_set.id;


create function validate_expansion_rule_activity_type()
returns trigger
security definer
language plpgsql as $$
begin
  raise exception 'PAUL' hint = 'PAUL HINT'
  return new;
end$$;

create trigger validate_expansion_rule_activity_type_trigger
before insert on expansion_set_to_rule
for each row
execute function validate_expansion_rule_activity_type();
