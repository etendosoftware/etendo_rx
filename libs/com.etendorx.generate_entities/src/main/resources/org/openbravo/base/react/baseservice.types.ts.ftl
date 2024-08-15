export type KV = {
  [key: string]: string | number | boolean | Date | undefined | null;
};

export type DASResponse<E extends EntityType> = {
  content: Array<EntityType>;
  pageable: Pageable;
  totalElements: number;
  totalPages: number;
  last: boolean;
  size: number;
  sort: KV;
  numberOfElements: number;
  first: boolean;
  empty: boolean;
};

export type Link = {
  [key: string]: string;
};

export interface EntityType {
  id?: string;
}

export type Pageable = {
  pageSize: number;
  totalElements: number;
  totalPages: number;
  pageNumber: number;
};
