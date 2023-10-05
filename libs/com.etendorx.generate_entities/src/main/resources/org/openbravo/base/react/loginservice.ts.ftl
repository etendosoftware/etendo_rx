import {KV} from '../base/baseservice.types';

export type SearchByUsernameParams = KV & {
  username: string;
  active: boolean;
};

export type SearchByIdParams = KV & {
  id: string;
};

export class LoginService {
  public async login(username: string, password: string) {
    const response = await fetch(`http://localhost:8094/api/authenticate`, {
      method: 'POST',
      headers: {
        Accept: 'application/json',
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({username: username, password: password}),
    });

    return response.json();
  }
}

export default LoginService;
