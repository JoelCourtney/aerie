alter table scheduling_specification_goals drop constraint scheduling_specification_unique_goal_id;
alter table scheduling_specification drop constraint scheduling_specification_unique_plan_id;

call migrations.mark_migration_rolled_back('2');
