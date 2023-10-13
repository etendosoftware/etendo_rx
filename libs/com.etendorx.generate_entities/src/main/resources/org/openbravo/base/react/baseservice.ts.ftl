import {DASResponse, EntityType, KV} from './baseservice.types';

export abstract class BaseService<E extends EntityType> {
  abstract getModelName(): string;
  abstract getFetchName(): string;
  abstract mapManyToOne(entity: E): void;
  _authToken: string = '';

  async save(entity: E, projection: string): Promise<E> {
    const _modelName = this.getModelName();
    let method = 'POST';
    let urlId = '';
    this.mapManyToOne(entity);
    if (entity.id !== undefined) {
      method = 'PATCH';
      urlId = entity.id;
    }

    const response = await fetch(
      `${"$"}{process.env.DAS_BASE_URL}/${"$"}{_modelName}/${"$"}{urlId}?projection=${"$"}{projection}`,
      {
        method: method,
        headers: {
          Accept: 'application/json',
          'Content-Type': 'application/json',
          'X-TOKEN': this._authToken,
        },
        body: JSON.stringify(entity),
      },
    );
    let responseText;
    try {
      responseText = await response.text();
      const responseEntity = JSON.parse(responseText);
      return responseEntity;
    } catch (e) {
      throw e;
    }
  }

  async delete(id: string | undefined): Promise<number> {
    const _modelName = this.getModelName();
    const response = await fetch(
      `${"$"}{process.env.DAS_BASE_URL}/${"$"}{_modelName}/${"$"}{id}`,
      {
        method: 'DELETE',
        headers: {
          Accept: 'application/json',
          'Content-Type': 'application/json',
          'X-TOKEN': this._authToken,
        },
      },
    );
    return response.status;
  }
  async _fetchSearch<S extends KV>(
    search: string,
    params: S,
    projection: string,
  ) {
    const _modelName = this.getModelName();
    const parsedParams = Object.keys(params)
      .map(k => {
        return `${"$"}{k}=${"$"}{params[k]}`;
      })
      .join('&');

    const res = await fetch(
      `${"$"}{process.env.DAS_BASE_URL}/${"$"}{_modelName}/search/${"$"}{search}?${"$"}{parsedParams}&projection=${projection}`,
    );
    const data = await res.json();
    const k = Object.keys(data._embedded);

    const returnedData = data._embedded[k[0]].map((u: DASResponse) => {
      delete u._links;
      return u;
    });
    return returnedData;
  }

  public get authToken() {
    return this._authToken;
  }

  public set authToken(authToken: string) {
    this._authToken = authToken;
  }
}
