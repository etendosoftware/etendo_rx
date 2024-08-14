import {DASResponse, EntityType, KV} from './baseservice.types';

export abstract class BaseService<E extends EntityType> {
  abstract getModelName(): string;
  abstract getFetchName(): string;
  abstract mapManyToOne(entity: E): void;
  _authToken: string = '';
  _url: string = '';

  async save(entity: E): Promise<E> {
    const _modelName = this.getModelName();
    let method = 'POST';
    let urlId = '';
    if (entity.id !== undefined) {
      method = 'PUT';
      urlId = "/" + entity.id;
    }

    const response = await fetch(`${'$'}{this._url}/das/${'$'}{_modelName}/${'$'}{urlId}`, {
      method: method,
      body: JSON.stringify(entity),
      headers: {
        Accept: 'application/json',
        'Content-Type': 'application/json',
        'X-TOKEN': this._authToken,
      },
    });
    if (!response.ok) {
      throw response;
    }

    const responseData = await response.json();
    return responseData;
  }

  async delete(id: string | undefined): Promise<number> {
    const _modelName = this.getModelName();
    const response = await fetch(`${'$'}{this._url}/das/${'$'}{_modelName}/${'$'}{id}`, {
      method: 'DELETE',
      headers: {
        Accept: 'application/json',
        'Content-Type': 'application/json',
        'X-TOKEN': this._authToken,
      },
    });
    return response.status;
  }
  async _fetchSearch<S extends KV>(search: string, params?: S) {
    const _modelName = this.getModelName();
    let parsedParams: string = '';
    if (params) {
      parsedParams = Object.keys(params)
        .map(k => {
          let v = params[k];
          if(v === null) {
            return '';
          }
          if(v instanceof Date) {
            // Transform date to string dd/mm/yyyy
            v = v.toLocaleDateString('es-AR');
          }
          if(k === 'page') {
            if(v === undefined) {
              v = 0;
            }
          }
          if(k === 'size') {
            if(v === undefined) {
              v = 20;
            }
          }
          return `${k}=${v}`
        })
        .join('&');
    }
    const res = await fetch(
      `${'$'}{this._url}/das/${'$'}{_modelName}/searches/${'$'}{search}?${'$'}{parsedParams}`,
      {
        method: 'GET',
        headers: {
          'X-TOKEN': this._authToken,
        },
      },
    );

    const data = await res.json();
    const response : DASResponse<E> = {
      content: data.content,
      pageable: data.pageable,
      totalElements: data.totalElements,
      totalPages: data.totalPages,
      last: data.last,
      size: data.size,
      sort: data.sort,
      numberOfElements: data.numberOfElements,
      first: data.first,
      empty: data.empty,
    };
    return response;
  }

  public get authToken() {
    return this._authToken;
  }

  public set authToken(authToken: string) {
    this._authToken = authToken;
  }

  public get url() {
    return this._url;
  }

  public set url(url: string) {
    this._url = url;
  }
}
