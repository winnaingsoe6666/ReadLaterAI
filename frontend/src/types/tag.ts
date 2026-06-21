export interface Tag {
  id: number;
  name: string;
}

export interface TagWithCount extends Tag {
  contentCount: number;
}
