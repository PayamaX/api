alter table public.predefined_origin rename to ranked_origin;
alter table public.ranked_origin add column comment varchar(400);
alter table public.ranked_origin add column updated Timestamp;
alter table public.ranked_origin add column deleted Timestamp;

alter table public.ranked_origin
	rename constraint uq__predefined_origin__global_id to uq__ranked_origin__global_id;

alter table public.ranked_origin
	drop constraint ix__predefined_origin__owner_global_id,
	drop constraint ix__predefined_origin__position;

create index ix__ranked_origin__owner_global_id
	on public.ranked_origin (owner_global_id);

create index ix__ranked_origin__position
	on public.ranked_origin ("position");
