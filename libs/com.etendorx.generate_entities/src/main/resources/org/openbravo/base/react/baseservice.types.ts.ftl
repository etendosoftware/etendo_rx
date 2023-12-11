export type KV = {
  [key: string]: string | number | boolean | undefined;
};

export type DASResponse = {
  _links?: Array<Link>;
};

export type Link = {
  [key: string]: string;
};

export interface EntityType {
  id?: string;
}
