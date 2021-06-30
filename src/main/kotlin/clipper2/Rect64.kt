package clipper2

class Rect64(val left: Long,val top: Long,val right: Long,val bottom: Long)
{
    constructor(r: Rect64) : this(r.left,r.top, r.right,r.bottom)
}
