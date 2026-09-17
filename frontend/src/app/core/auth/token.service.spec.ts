import { TestBed } from '@angular/core/testing';import { TokenService } from './token.service';
describe('TokenService',()=>{let service:TokenService;beforeEach(()=>{TestBed.configureTestingModule({});service=TestBed.inject(TokenService);localStorage.clear();});
it('accepts an unexpired JWT',()=>{const payload=btoa(JSON.stringify({exp:Math.floor(Date.now()/1000)+60})).replace(/=/g,'');service.setToken(`x.${payload}.x`);expect(service.hasToken()).toBe(true);});
it('clears expired or malformed JWTs',()=>{const payload=btoa(JSON.stringify({exp:1})).replace(/=/g,'');service.setToken(`x.${payload}.x`);expect(service.hasToken()).toBe(false);expect(service.getToken()).toBeNull();service.setToken('invalid');expect(service.hasToken()).toBe(false);});});
