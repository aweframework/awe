import {resolveIcon} from '../../../src/utilities/iconResolver';

const lucide = (name, classes = '') => ({family: 'lucide', name, classes});

describe('resolveIcon (#885)', () => {
  let warn;

  beforeEach(() => {
    warn = jest.spyOn(console, 'warn').mockImplementation(() => {});
  });

  afterEach(() => {
    warn.mockRestore();
  });

  it('maps a Font Awesome 4 name with or without the fa- prefix or the fa: family', () => {
    expect(resolveIcon('trash')).toEqual(lucide('trash'));
    expect(resolveIcon('fa-trash')).toEqual(lucide('trash'));
    expect(resolveIcon('fa:floppy-o')).toEqual(lucide('save'));
    expect(resolveIcon('fa:fa-print')).toEqual(lucide('printer'));
  });

  it('follows the table of the foundations page', () => {
    expect(resolveIcon('search')).toEqual(lucide('search'));
    expect(resolveIcon('plus')).toEqual(lucide('plus'));
    expect(resolveIcon('pencil')).toEqual(lucide('pencil'));
    expect(resolveIcon('edit')).toEqual(lucide('pencil'));
    expect(resolveIcon('filter')).toEqual(lucide('funnel'));
    expect(resolveIcon('download')).toEqual(lucide('download'));
    expect(resolveIcon('times')).toEqual(lucide('x'));
    expect(resolveIcon('cog')).toEqual(lucide('settings'));
  });

  it('maps a Google Material Icons ligature of the mdi: family', () => {
    expect(resolveIcon('mdi:dark_mode')).toEqual(lucide('moon'));
    expect(resolveIcon('mdi:person_outline')).toEqual(lucide('user'));
    expect(resolveIcon('mdi:home')).toEqual(lucide('house'));
  });

  it('maps a PrimeIcons name written by the React components', () => {
    expect(resolveIcon('pi-question-circle')).toEqual(lucide('circle-question-mark'));
    expect(resolveIcon('pi:times')).toEqual(lucide('x'));
  });

  it('takes a lucide: name as it is and an alias as its current name', () => {
    expect(resolveIcon('lucide:house')).toEqual(lucide('house'));
    expect(resolveIcon('lucide:home')).toEqual(lucide('house'));
    expect(resolveIcon('lucide:trash-2')).toEqual(lucide('trash'));
    expect(warn).not.toHaveBeenCalled();
  });

  it('keeps the font of a fallback or unknown name', () => {
    expect(resolveIcon('rebel')).toEqual({family: 'fa', name: 'rebel', classes: ''});
    expect(resolveIcon('fa-rebel')).toEqual({family: 'fa', name: 'rebel', classes: ''});
    expect(resolveIcon('not-an-icon')).toEqual({family: 'fa', name: 'not-an-icon', classes: ''});
    expect(resolveIcon('mdi:not_an_icon')).toEqual({family: 'material', name: 'not_an_icon', classes: ''});
    expect(resolveIcon('pi-not-an-icon')).toEqual({family: 'pi', name: 'not-an-icon', classes: ''});
    expect(warn).not.toHaveBeenCalled();
  });

  it('only looks up the icon names of the tables', () => {
    expect(resolveIcon('constructor')).toEqual({family: 'fa', name: 'constructor', classes: ''});
    expect(resolveIcon('fallback:fa')).toEqual({family: 'fallback', name: 'fa', classes: ''});
    expect(resolveIcon('lucide:constructor')).toBeNull();
  });

  it('returns null for an unknown lucide: name and warns once per name', () => {
    expect(resolveIcon('lucide:not-an-icon')).toBeNull();
    expect(resolveIcon('lucide:not-an-icon')).toBeNull();
    expect(warn).toHaveBeenCalledTimes(1);
    expect(warn.mock.calls[0][0]).toContain('lucide:not-an-icon');
    expect(resolveIcon('lucide:another-unknown')).toBeNull();
    expect(warn).toHaveBeenCalledTimes(2);
  });

  it('keeps the classes written after the icon name', () => {
    expect(resolveIcon('level-up icon-rotate-90')).toEqual(lucide('corner-right-up', 'icon-rotate-90'));
    expect(resolveIcon('  bars icon navbar-icon ')).toEqual(lucide('menu', 'icon navbar-icon'));
    expect(resolveIcon('rebel big')).toEqual({family: 'fa', name: 'rebel', classes: 'big'});
  });

  it('returns null without an icon', () => {
    expect(resolveIcon('')).toBeNull();
    expect(resolveIcon('   ')).toBeNull();
    expect(resolveIcon(null)).toBeNull();
    expect(resolveIcon(undefined)).toBeNull();
    expect(resolveIcon('lucide:')).toBeNull();
  });
});
