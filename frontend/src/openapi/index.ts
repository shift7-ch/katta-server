import o from './openapi.json';
export const openapi = o;

export type OpenapiType = {
  description?: string;
  type?: string;
  example?: string;
  nullable?: boolean;
  allOf?: OpenapiRef[];
}

export type OpenapiRef = {
  "$ref": string;
}

export type OpenapiSchema = {
  pattern?: string;
  enum?: any;
}
export type OpenapiSchemas = Record<string,any>;
